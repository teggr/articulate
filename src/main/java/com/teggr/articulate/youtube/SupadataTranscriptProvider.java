package com.teggr.articulate.youtube;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.teggr.articulate.exception.TranscriptNotFoundException;
import com.teggr.articulate.service.transcripts.TranscriptProvider;
import com.teggr.articulate.service.transcripts.TranscriptResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches YouTube transcripts through Supadata's HTTP API.
 */
@Component
@Slf4j
public class SupadataTranscriptProvider implements TranscriptProvider {

    private static final String DEFAULT_BASE_URL = "https://api.supadata.ai/v1";
    private static final String UNKNOWN_TITLE = "Unknown Title";

    private final ObjectMapper objectMapper;
    private final YouTubeVideoIdExtractor videoIdExtractor;
    private final RestClient restClient;
    private final String apiKey;
    private final int pollMaxAttempts;
    private final Duration pollInterval;

    @Autowired
    public SupadataTranscriptProvider(
            ObjectMapper objectMapper,
            YouTubeVideoIdExtractor videoIdExtractor,
            @Value("${supadata.api-key:}") String apiKey,
            @Value("${supadata.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${supadata.poll.max-attempts:10}") int pollMaxAttempts,
            @Value("${supadata.poll.interval-ms:1000}") long pollIntervalMs) {
        this(
                objectMapper,
                videoIdExtractor,
                apiKey,
                buildRestClient(baseUrl, apiKey),
                pollMaxAttempts,
                Duration.ofMillis(Math.max(100, pollIntervalMs))
        );
    }

    SupadataTranscriptProvider(
            ObjectMapper objectMapper,
            YouTubeVideoIdExtractor videoIdExtractor,
            String apiKey,
            RestClient restClient,
            int pollMaxAttempts,
            Duration pollInterval) {
        this.objectMapper = objectMapper;
        this.videoIdExtractor = videoIdExtractor;
        this.apiKey = apiKey;
        this.restClient = restClient;
        this.pollMaxAttempts = Math.max(1, pollMaxAttempts);
        this.pollInterval = pollInterval;
    }

    private static RestClient buildRestClient(String baseUrl, String apiKey) {
        String resolvedBaseUrl = StringUtils.hasText(baseUrl) ? baseUrl : DEFAULT_BASE_URL;
        return RestClient.builder()
                .baseUrl(resolvedBaseUrl)
                .defaultHeader("x-api-key", apiKey == null ? "" : apiKey)
                .defaultHeader("Accept", "application/json")
                .build();
    }

    @Override
    public TranscriptResult fetchTranscript(String youtubeUrl) {
        validateApiKey();

        String videoId = videoIdExtractor.extract(youtubeUrl);
        JsonNode transcriptPayload = requestTranscript(youtubeUrl);

        String transcript = extractTranscriptText(transcriptPayload.path("content")).strip();
        if (transcript.isBlank()) {
            throw new TranscriptNotFoundException("No transcript content was returned by Supadata");
        }

        String title = extractTitle(transcriptPayload);
        return TranscriptResult.fetched(videoId, title, transcript);
    }

    private JsonNode requestTranscript(String youtubeUrl) {
        try {
            ResponseEntity<String> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/transcript")
                    .queryParam("url", youtubeUrl)
                    .queryParam("mode", "native")
                    .queryParam("text", true)
                    .build())
                    .retrieve()
                    .toEntity(String.class);

            JsonNode body = parseJsonBody(response.getBody());
            if (response.getStatusCode().value() == 202) {
                String jobId = extractJobId(body);
                return resolveTranscriptPayload(pollForTranscriptJob(jobId));
            }
            return resolveTranscriptPayload(body);
        } catch (RestClientResponseException e) {
            throw mapSupadataError(e);
        }
    }

    private JsonNode pollForTranscriptJob(String jobId) {
        for (int attempt = 1; attempt <= pollMaxAttempts; attempt++) {
            try {
                ResponseEntity<String> response = restClient.get()
                        .uri("/transcript/{jobId}", jobId)
                        .retrieve()
                        .toEntity(String.class);

                JsonNode body = parseJsonBody(response.getBody());
                if (looksLikeTranscriptPayload(body)) {
                    return body;
                }

                JsonNode nestedPayload = findTranscriptPayload(body);
                if (nestedPayload != null) {
                    return nestedPayload;
                }

                if (response.getStatusCode().value() == 202 || isPending(body)) {
                    if (attempt == pollMaxAttempts) {
                        break;
                    }
                    sleepPollInterval();
                    continue;
                }

                throw new RuntimeException("Supadata transcript job returned an unexpected response structure");
            } catch (RestClientResponseException e) {
                throw mapSupadataError(e);
            }
        }

        throw new TranscriptNotFoundException(
                "Transcript is still being prepared by Supadata. Please try again in a few moments.");
    }

    private void sleepPollInterval() {
        try {
            Thread.sleep(pollInterval.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for Supadata transcript job", e);
        }
    }

    private JsonNode resolveTranscriptPayload(JsonNode root) {
        if (looksLikeTranscriptPayload(root)) {
            return root;
        }

        JsonNode nested = findTranscriptPayload(root);
        if (nested != null) {
            return nested;
        }

        throw new TranscriptNotFoundException("Supadata transcript response did not include transcript content");
    }

    private JsonNode findTranscriptPayload(JsonNode root) {
        for (String field : List.of("content", "data", "result", "transcript", "output", "payload")) {
            JsonNode node = root.path(field);
            if (node.isMissingNode() || node.isNull()) {
                continue;
            }
            if (looksLikeTranscriptPayload(node)) {
                return node;
            }
            JsonNode nested = findTranscriptPayload(node);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private boolean looksLikeTranscriptPayload(JsonNode node) {
        return node != null
                && node.isObject()
                && node.has("content")
                && !node.path("content").isMissingNode()
                && !node.path("content").isNull();
    }

    private boolean isPending(JsonNode body) {
        String status = body.path("status").asText("").toLowerCase();
        return status.equals("pending") || status.equals("queued") || status.equals("processing") || status.equals("running");
    }

    private String extractJobId(JsonNode body) {
        String jobId = body.path("jobId").asText("");
        if (!jobId.isBlank()) {
            return jobId;
        }
        jobId = body.path("id").asText("");
        if (!jobId.isBlank()) {
            return jobId;
        }
        throw new RuntimeException("Supadata returned async response without a jobId");
    }

    private JsonNode parseJsonBody(String responseBody) {
        try {
            if (!StringUtils.hasText(responseBody)) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Supadata response JSON", e);
        }
    }

    private String extractTranscriptText(JsonNode contentNode) {
        if (contentNode == null || contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isObject()) {
            if (contentNode.has("text")) {
                return contentNode.path("text").asText("");
            }
            if (contentNode.has("content")) {
                return extractTranscriptText(contentNode.path("content"));
            }
            return "";
        }
        if (!contentNode.isArray()) {
            return "";
        }

        List<String> chunks = new ArrayList<>();
        for (JsonNode item : contentNode) {
            if (item.isTextual()) {
                String value = item.asText("").strip();
                if (!value.isBlank()) {
                    chunks.add(value);
                }
                continue;
            }
            if (item.isObject()) {
                String value = item.path("text").asText("").strip();
                if (!value.isBlank()) {
                    chunks.add(value);
                }
            }
        }

        return String.join(" ", chunks);
    }

    private String extractTitle(JsonNode payload) {
        for (String path : List.of(
                "/title",
                "/video/title",
                "/videoTitle",
                "/metadata/title",
                "/meta/title"
        )) {
            String title = payload.at(path).asText("").strip();
            if (!title.isBlank()) {
                return title;
            }
        }
        return UNKNOWN_TITLE;
    }

    private void validateApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new RuntimeException("SUPADATA_API_KEY is not configured");
        }
    }

    private RuntimeException mapSupadataError(RestClientResponseException e) {
        int code = e.getStatusCode().value();
        return switch (code) {
            case 401 -> new RuntimeException("Supadata API key is missing or invalid. Set SUPADATA_API_KEY.", e);
            case 402 -> new RuntimeException("Supadata billing is required for transcript requests (HTTP 402).", e);
            case 404 -> new TranscriptNotFoundException("No transcript is available for this video.");
            case 429 -> new TranscriptNotFoundException(
                    "Supadata is rate-limiting transcript requests right now. Please wait a few minutes and try again.");
            default -> new RuntimeException("Supadata transcript request failed with HTTP " + code, e);
        };
    }
}
