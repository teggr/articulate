package com.teggr.articluate.youtube;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.teggr.articluate.exception.TranscriptNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
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

    /**
     * Matches the 11-character video ID in the common YouTube URL formats:
     * watch?v=, youtu.be/, shorts/, embed/, v/
     */
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/|v/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    private static final Pattern BARE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public YoutubeTranscriptProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        String title = extractTitle(playerResponse);
        String captionUrl = extractCaptionUrl(playerResponse);
        String xml = fetchTranscriptXml(captionUrl);
        String transcript = parseTranscriptXml(xml);

        log.debug("Fetched {} characters of transcript for \"{}\"", transcript.length(), title);
        return new TranscriptResult(videoId, title, transcript);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String extractVideoId(String input) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (BARE_ID_PATTERN.matcher(input).matches()) {
            return input;
        }
        throw new IllegalArgumentException("Cannot extract a YouTube video ID from: " + input);
    }

    private String fetchPage(String videoId) {
        try {
            return restClient.get()
                    .uri(YOUTUBE_WATCH_URL + videoId)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch YouTube page for video ID: " + videoId, e);
        }
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
        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch transcript XML", e);
        }
    }

    /**
     * Parses the YouTube timed-text XML (series of {@code <text>} elements)
     * into a single plain-text string.
     */
    private String parseTranscriptXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable DOCTYPE to prevent XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));

            NodeList textNodes = doc.getElementsByTagName("text");
            StringBuilder sb = new StringBuilder(textNodes.getLength() * 60);

            for (int i = 0; i < textNodes.getLength(); i++) {
                String text = textNodes.item(i).getTextContent()
                        .replace('\n', ' ')
                        .strip();
                if (!text.isEmpty()) {
                    sb.append(text).append(' ');
                }
            }

            return sb.toString().strip();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse transcript XML: " + e.getMessage(), e);
        }
    }
}
