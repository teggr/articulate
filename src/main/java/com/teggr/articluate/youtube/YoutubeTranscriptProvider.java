package com.teggr.articluate.youtube;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.teggr.articluate.exception.TranscriptNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches YouTube transcripts by:
 * <ol>
 *   <li>Scraping the {@code ytInitialPlayerResponse} JSON from the watch page</li>
 *   <li>Locating the caption-track URL embedded in that JSON</li>
 *   <li>Fetching and parsing the timed-text XML</li>
 * </ol>
 */
@Component
@Slf4j
public class YoutubeTranscriptProvider implements TranscriptProvider {

    private static final String YOUTUBE_WATCH_URL = "https://www.youtube.com/watch?v=";
    private static final String YOUTUBE_PLAYER_API_URL = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false&key=";
    private static final String YOUTUBE_TIMEDTEXT_URL = "https://video.google.com/timedtext";
    private static final String ANDROID_CLIENT_VERSION = "20.22.34";
    private static final String ANDROID_USER_AGENT = "com.google.android.youtube/20.22.34 (Linux; U; Android 11) gzip";
    private static final String ANDROID_CLIENT_NAME_HEADER = "3";
    private static final int HTTP_429_RETRY_COUNT = 3;
    private static final Pattern INNERTUBE_API_KEY_PATTERN = Pattern.compile("\"INNERTUBE_API_KEY\"\\s*:\\s*\"([a-zA-Z0-9_-]+)\"");
    private static final Pattern CONSENT_VALUE_PATTERN = Pattern.compile("name=\"v\" value=\"(.*?)\"");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final YouTubeVideoIdExtractor videoIdExtractor;

    @Autowired
    public YoutubeTranscriptProvider(ObjectMapper objectMapper) {
        this(objectMapper, new YouTubeVideoIdExtractor());
    }

    YoutubeTranscriptProvider(ObjectMapper objectMapper, YouTubeVideoIdExtractor videoIdExtractor) {
        this.objectMapper = objectMapper;
        this.videoIdExtractor = videoIdExtractor;
        this.restClient = RestClient.builder()
                .defaultHeader("Accept-Language", "en-US,en;q=0.9")
                .defaultHeader("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/124.0.0.0 Safari/537.36")
            .build();
    }

    @Override
    public TranscriptResult fetchTranscript(String youtubeUrl) {
        String videoId = extractVideoId(youtubeUrl);
        log.debug("Fetching transcript for video ID: {}", videoId);

        String pageHtml = fetchPage(videoId);
        JsonNode playerResponse = extractPlayerResponse(pageHtml);
        assertPlayability(playerResponse, videoId);

        String title = extractTitle(playerResponse);
        String xml = null;

        try {
            String captionUrl = extractCaptionUrl(playerResponse);
            xml = fetchTranscriptXml(captionUrl);
        } catch (RuntimeException webCaptionError) {
            log.debug("Web caption fetch failed for {}. Falling back to Android caption endpoint", videoId, webCaptionError);
        }

        if (xml == null || xml.isBlank()) {
            try {
                JsonNode androidPlayerResponse = fetchAndroidPlayerResponse(videoId, pageHtml);
                assertPlayability(androidPlayerResponse, videoId);
                String androidCaptionUrl = extractCaptionUrl(androidPlayerResponse);
                xml = fetchTranscriptXml(androidCaptionUrl);
            } catch (RuntimeException androidCaptionError) {
                log.debug("Android caption fetch failed for {}. Falling back to timedtext track list", videoId, androidCaptionError);
            }
        }

        if (xml == null || xml.isBlank()) {
            try {
                String timedtextTrackUrl = extractTimedTextTrackUrl(videoId);
                xml = fetchTranscriptXml(timedtextTrackUrl);
            } catch (RuntimeException timedtextListError) {
                log.debug("Timedtext track-list flow failed for {}. Falling back to direct timedtext URL guesses", videoId, timedtextListError);
            }
        }

        if (xml == null || xml.isBlank()) {
            xml = fetchDirectTimedTextXml(videoId);
        }

        if (xml == null || xml.isBlank()) {
            throw new RuntimeException("Failed to fetch transcript XML for video ID: " + videoId);
        }

        String transcript = parseTranscriptXml(xml);

        log.debug("Fetched {} characters of transcript for \"{}\"", transcript.length(), title);
        return new TranscriptResult(videoId, title, transcript);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    String extractVideoId(String input) {
        return videoIdExtractor.extract(input);
    }

    private String fetchPage(String videoId) {
        try {
            String html = restClient.get()
                    .uri(YOUTUBE_WATCH_URL + videoId)
                    .retrieve()
                    .body(String.class);

            if (html == null || html.isBlank()) {
                throw new RuntimeException("YouTube watch page was empty");
            }

            if (html.contains("class=\"g-recaptcha\"") || html.contains("Sign in to confirm you\u2019re not a bot")) {
                throw new TranscriptNotFoundException(
                        "YouTube is blocking requests from this server IP (bot check triggered)");
            }

            if (html.contains("action=\"https://consent.youtube.com/s\"")) {
                String consentCookie = createConsentCookie(html);
                html = restClient.get()
                        .uri(YOUTUBE_WATCH_URL + videoId)
                        .header(HttpHeaders.COOKIE, consentCookie)
                        .retrieve()
                        .body(String.class);

                if (html == null || html.isBlank()) {
                    throw new RuntimeException("YouTube watch page was empty after consent flow");
                }
            }

            return html;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new TranscriptNotFoundException(
                        "YouTube is rate-limiting requests from this server IP (HTTP 429)");
            }
            throw new RuntimeException("Failed to fetch YouTube page for video ID: " + videoId, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch YouTube page for video ID: " + videoId, e);
        }
    }

    private String createConsentCookie(String html) {
        Matcher matcher = CONSENT_VALUE_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new TranscriptNotFoundException("Failed to create YouTube consent cookie");
        }
        return "CONSENT=YES+" + matcher.group(1);
    }

    /**
     * Extracts the {@code ytInitialPlayerResponse} JSON blob from the watch-page HTML
     * by matching braces — this avoids a full HTML parser dependency.
     */
    private JsonNode extractPlayerResponse(String html) {
        int varIdx = html.indexOf("ytInitialPlayerResponse");
        if (varIdx == -1) {
            throw new TranscriptNotFoundException(
                    "ytInitialPlayerResponse not found — YouTube may have changed its page structure");
        }
        int jsonStart = html.indexOf('{', varIdx);
        if (jsonStart == -1) {
            throw new TranscriptNotFoundException("Malformed ytInitialPlayerResponse (no opening brace)");
        }

        int depth = 0;
        int jsonEnd = -1;
        for (int i = jsonStart; i < html.length(); i++) {
            char c = html.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    jsonEnd = i + 1;
                    break;
                }
            }
        }

        if (jsonEnd == -1) {
            throw new TranscriptNotFoundException("Could not find matching closing brace for ytInitialPlayerResponse");
        }

        try {
            return objectMapper.readTree(html.substring(jsonStart, jsonEnd));
        } catch (Exception e) {
            throw new TranscriptNotFoundException("Failed to parse ytInitialPlayerResponse JSON: " + e.getMessage());
        }
    }

    private String extractTitle(JsonNode playerResponse) {
        JsonNode node = playerResponse.at("/videoDetails/title");
        return node.isMissingNode() ? "Unknown Title" : node.asText();
    }

    /**
     * Locates the best-matching English caption track URL.
     * Prefers manually created tracks over auto-generated ones ({@code a.en}).
     */
    private String extractCaptionUrl(JsonNode playerResponse) {
        JsonNode tracks = playerResponse.at("/captions/playerCaptionsTracklistRenderer/captionTracks");
        if (tracks.isMissingNode() || tracks.isEmpty()) {
            throw new TranscriptNotFoundException(
                    "No captions available for this video — the owner may have disabled them");
        }

        JsonNode chosen = null;

        // 1. Manual English track
        for (JsonNode track : tracks) {
            String langCode = track.path("languageCode").asText("");
            String vssId = track.path("vssId").asText("");
            if (langCode.startsWith("en") && !vssId.startsWith("a.")) {
                chosen = track;
                break;
            }
        }

        // 2. Auto-generated English track
        if (chosen == null) {
            for (JsonNode track : tracks) {
                if (track.path("languageCode").asText("").startsWith("en")) {
                    chosen = track;
                    break;
                }
            }
        }

        // 3. First available track
        if (chosen == null) {
            chosen = tracks.get(0);
        }

        String baseUrl = chosen.path("baseUrl").asText("");
        if (baseUrl.isBlank()) {
            throw new TranscriptNotFoundException("Caption base URL is empty");
        }
        return baseUrl;
    }

    private String fetchTranscriptXml(String url) {
        List<String> attemptSummaries = new ArrayList<>();
        Exception lastError = null;

        for (String candidateUrl : buildTranscriptUrlCandidates(url)) {
            for (int attempt = 1; attempt <= HTTP_429_RETRY_COUNT; attempt++) {
                try {
                    ResponseEntity<String> response = restClient.get()
                            .uri(candidateUrl)
                            .retrieve()
                            .toEntity(String.class);

                    String body = response.getBody();
                    int bodyLength = body == null ? 0 : body.length();
                    attemptSummaries.add("status=" + response.getStatusCode()
                            + ", len=" + bodyLength
                            + ", attempt=" + attempt
                            + ", url=" + summarizeUrl(candidateUrl)
                            + ", preview='" + bodyPreview(body) + "'");

                    if (body != null && !body.isBlank()) {
                        return body;
                    }
                    break;
                } catch (RestClientResponseException e) {
                    lastError = new RuntimeException("HTTP " + e.getStatusCode()
                            + " while fetching transcript XML");
                    attemptSummaries.add("error=HTTP " + e.getStatusCode()
                            + ", len=" + e.getResponseBodyAsString().length()
                            + ", attempt=" + attempt
                            + ", preview='" + bodyPreview(e.getResponseBodyAsString()) + "'"
                            + ", url=" + summarizeUrl(candidateUrl));

                    if (e.getStatusCode().value() == 429 && attempt < HTTP_429_RETRY_COUNT) {
                        continue;
                    }
                    break;
                } catch (Exception e) {
                    lastError = new RuntimeException("Unexpected error while fetching transcript XML", e);
                    attemptSummaries.add("error=" + e.getClass().getSimpleName()
                            + ", message='" + (e.getMessage() == null ? "n/a" : e.getMessage()) + "'"
                            + ", attempt=" + attempt
                            + ", url=" + summarizeUrl(candidateUrl));
                    break;
                }
            }
        }

        log.debug("Transcript XML fetch attempts: {}", String.join(" | ", attemptSummaries));

        String message = "Failed to fetch transcript XML after " + attemptSummaries.size()
                + " attempts from: " + summarizeUrl(url);
        throw new RuntimeException(message, lastError);
    }

    private JsonNode fetchAndroidPlayerResponse(String videoId, String pageHtml) {
        String apiKey = extractInnertubeApiKey(pageHtml);

        try {
            String responseBody = restClient.post()
                    .uri(YOUTUBE_PLAYER_API_URL + apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.USER_AGENT, ANDROID_USER_AGENT)
                    .header("X-YouTube-Client-Name", ANDROID_CLIENT_NAME_HEADER)
                    .header("X-YouTube-Client-Version", ANDROID_CLIENT_VERSION)
                    .body("{" +
                            "\"context\":{" +
                            "\"client\":{" +
                            "\"clientName\":\"ANDROID\"," +
                            "\"clientVersion\":\"" + ANDROID_CLIENT_VERSION + "\"," +
                            "\"hl\":\"en\"," +
                            "\"gl\":\"US\"," +
                            "\"androidSdkVersion\":30," +
                            "\"osName\":\"Android\"," +
                            "\"osVersion\":\"11\"," +
                            "\"platform\":\"MOBILE\"" +
                            "}}," +
                            "\"videoId\":\"" + videoId + "\"" +
                            "}")
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("Android player response was empty");
            }
            return objectMapper.readTree(responseBody);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                throw new TranscriptNotFoundException(
                        "YouTube is rate-limiting requests from this server IP (HTTP 429)");
            }
            throw new RuntimeException("Failed to fetch Android player response", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Android player response", e);
        }
    }

    private String extractInnertubeApiKey(String html) {
        Matcher matcher = INNERTUBE_API_KEY_PATTERN.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (html.contains("class=\"g-recaptcha\"") || html.contains("Sign in to confirm you\u2019re not a bot")) {
            throw new TranscriptNotFoundException(
                    "YouTube is blocking requests from this server IP (bot check triggered)");
        }
        throw new RuntimeException("INNERTUBE_API_KEY not found in YouTube watch page");
    }

    private void assertPlayability(JsonNode playerResponse, String videoId) {
        JsonNode playability = playerResponse.path("playabilityStatus");
        if (playability.isMissingNode()) {
            return;
        }

        String status = playability.path("status").asText("");
        if (status.isBlank() || "OK".equals(status)) {
            return;
        }

        String reason = playability.path("reason").asText("unknown reason");
        if ("LOGIN_REQUIRED".equals(status) && reason.contains("not a bot")) {
            throw new TranscriptNotFoundException(
                    "YouTube is blocking requests from this server IP (bot check): " + reason);
        }
        if ("LOGIN_REQUIRED".equals(status) && reason.contains("inappropriate for some users")) {
            throw new TranscriptNotFoundException("Video is age-restricted and cannot be transcribed anonymously");
        }
        if ("ERROR".equals(status) && reason.toLowerCase().contains("unavailable")) {
            throw new TranscriptNotFoundException("Video is unavailable: " + videoId);
        }

        throw new TranscriptNotFoundException("Video is not playable right now: " + reason);
    }

    private String extractTimedTextTrackUrl(String videoId) {
        String listUrl = UriComponentsBuilder.fromUriString(YOUTUBE_TIMEDTEXT_URL)
                .queryParam("type", "list")
                .queryParam("v", videoId)
                .build(true)
                .toUriString();

        String listXml = restClient.get()
                .uri(listUrl)
                .retrieve()
                .body(String.class);

        if (listXml == null || listXml.isBlank()) {
            throw new TranscriptNotFoundException("Timedtext track list is empty");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(listXml)));
            NodeList trackNodes = doc.getElementsByTagName("track");

            if (trackNodes.getLength() == 0) {
                throw new TranscriptNotFoundException(
                        "No captions available for this video — the owner may have disabled them");
            }

            String langCode = null;
            String trackName = null;

            // Prefer English track first.
            for (int i = 0; i < trackNodes.getLength(); i++) {
                var node = trackNodes.item(i);
                var attributes = node.getAttributes();
                if (attributes == null) {
                    continue;
                }
                var lang = attributes.getNamedItem("lang_code");
                if (lang != null && lang.getNodeValue() != null && lang.getNodeValue().startsWith("en")) {
                    langCode = lang.getNodeValue();
                    var nameNode = attributes.getNamedItem("name");
                    trackName = nameNode == null ? null : nameNode.getNodeValue();
                    break;
                }
            }

            // If no English track exists, take the first available language.
            if (langCode == null) {
                var firstAttributes = trackNodes.item(0).getAttributes();
                if (firstAttributes != null) {
                    var firstLang = firstAttributes.getNamedItem("lang_code");
                    if (firstLang != null) {
                        langCode = firstLang.getNodeValue();
                    }
                    var firstName = firstAttributes.getNamedItem("name");
                    trackName = firstName == null ? null : firstName.getNodeValue();
                }
            }

            if (langCode == null || langCode.isBlank()) {
                throw new TranscriptNotFoundException("No usable timedtext track found for this video");
            }

            UriComponentsBuilder transcriptUrlBuilder = UriComponentsBuilder.fromUriString(YOUTUBE_TIMEDTEXT_URL)
                    .queryParam("v", videoId)
                    .queryParam("lang", langCode)
                    .queryParam("fmt", "xml3");

            if (trackName != null && !trackName.isBlank()) {
                transcriptUrlBuilder.queryParam("name", trackName);
            }

            return transcriptUrlBuilder
                    .build(true)
                    .toUriString();
        } catch (TranscriptNotFoundException ex) {
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse timedtext track list", e);
        }
    }

    private String fetchDirectTimedTextXml(String videoId) {
        List<String> candidates = List.of(
                UriComponentsBuilder.fromUriString(YOUTUBE_TIMEDTEXT_URL)
                        .queryParam("v", videoId)
                        .queryParam("lang", "en")
                        .queryParam("fmt", "xml3")
                        .build(true)
                        .toUriString(),
                UriComponentsBuilder.fromUriString(YOUTUBE_TIMEDTEXT_URL)
                        .queryParam("v", videoId)
                        .queryParam("lang", "en-US")
                        .queryParam("fmt", "xml3")
                        .build(true)
                        .toUriString(),
                UriComponentsBuilder.fromUriString(YOUTUBE_TIMEDTEXT_URL)
                        .queryParam("v", videoId)
                        .queryParam("lang", "en")
                        .queryParam("kind", "asr")
                        .queryParam("fmt", "xml3")
                        .build(true)
                        .toUriString(),
                UriComponentsBuilder.fromUriString(YOUTUBE_TIMEDTEXT_URL)
                        .queryParam("v", videoId)
                        .queryParam("lang", "en-US")
                        .queryParam("kind", "asr")
                        .queryParam("fmt", "xml3")
                        .build(true)
                        .toUriString()
        );

        RuntimeException lastError = null;
        for (String candidate : candidates) {
            try {
                return fetchTranscriptXml(candidate);
            } catch (RuntimeException ex) {
                lastError = ex;
            }
        }

        throw new TranscriptNotFoundException(
            "No captions available for this video — timedtext list and direct timedtext lookups failed"
                + (lastError == null ? "" : ": " + lastError.getMessage()));
    }

    private List<String> buildTranscriptUrlCandidates(String originalUrl) {
        Set<String> urls = new LinkedHashSet<>();
        urls.add(originalUrl);

        String reduced = UriComponentsBuilder.fromUriString(originalUrl)
                .replaceQueryParam("variant")
                .build(true)
                .toUriString();
        urls.add(reduced);

        urls.add(UriComponentsBuilder.fromUriString(reduced)
                .replaceQueryParam("fmt", "xml3")
                .build(true)
                .toUriString());

        urls.add(UriComponentsBuilder.fromUriString(reduced)
                .replaceQueryParam("tlang", "en")
                .build(true)
                .toUriString());

        urls.add(UriComponentsBuilder.fromUriString(reduced)
                .replaceQueryParam("fmt", "xml3")
                .replaceQueryParam("tlang", "en")
                .build(true)
                .toUriString());

        return List.copyOf(urls);
    }

    private String bodyPreview(String body) {
        if (body == null) {
            return "null";
        }
        String compact = body.replace('\n', ' ').replace('\r', ' ').strip();
        return compact.length() <= 120 ? compact : compact.substring(0, 120) + "...";
    }

    private String summarizeUrl(String url) {
        try {
            var components = UriComponentsBuilder.fromUriString(url).build(true);
            String queryKeys = String.join(",", components.getQueryParams().keySet());
            if (queryKeys.isBlank()) {
                return components.getScheme() + "://" + components.getHost() + components.getPath();
            }
            return components.getScheme() + "://" + components.getHost() + components.getPath() + "?" + queryKeys;
        } catch (Exception ignored) {
            return url;
        }
    }

    /**
     * Parses the YouTube timed-text XML (series of {@code <text>} elements)
     * into a single plain-text string.
     */
    private String parseTranscriptXml(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new RuntimeException("Failed to parse transcript XML: input was null or blank");
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable DOCTYPE to prevent XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList textNodes = doc.getElementsByTagName("text");
            if (textNodes.getLength() == 0) {
                textNodes = doc.getElementsByTagName("p");
            }

            StringBuilder sb = new StringBuilder(textNodes.getLength() * 60);

            for (int i = 0; i < textNodes.getLength(); i++) {
                String rawText = textNodes.item(i).getTextContent();
                if (rawText == null) {
                    continue;
                }

                String text = rawText
                        .replace('\n', ' ')
                        .strip();
                if (!text.isEmpty()) {
                    sb.append(text).append(' ');
                }
            }

            return sb.toString().strip();
        } catch (Exception e) {
            String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new RuntimeException("Failed to parse transcript XML: " + message, e);
        }
    }
}
