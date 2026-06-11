package com.teggr.articulate.youtube;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.teggr.articulate.service.transcripts.TranscriptResult;
import com.teggr.articulate.youtube.SupadataTranscriptProvider;
import com.teggr.articulate.youtube.YouTubeVideoIdExtractor;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SupadataTranscriptProviderIntegrationTest {

    @Test
    @EnabledIfSystemProperty(named = "supadata.integration", matches = "true")
    void fetchTranscriptFromSupadata() {
        String apiKey = System.getenv("SUPADATA_API_KEY");
        SupadataTranscriptProvider provider = new SupadataTranscriptProvider(
                new ObjectMapper(),
                new YouTubeVideoIdExtractor(),
                apiKey,
                org.springframework.web.client.RestClient.builder()
                        .baseUrl("https://api.supadata.ai/v1")
                        .defaultHeader("x-api-key", apiKey == null ? "" : apiKey)
                        .build(),
                10,
                java.time.Duration.ofSeconds(1)
        );

        TranscriptResult result = provider.fetchTranscript("https://www.youtube.com/watch?v=nfIcjkR4KZ8");

        assertEquals("nfIcjkR4KZ8", result.videoId());
        assertNotNull(result.title());
        assertFalse(result.title().isBlank());
        assertNotNull(result.transcript());
        assertFalse(result.transcript().isBlank());
    }
}
