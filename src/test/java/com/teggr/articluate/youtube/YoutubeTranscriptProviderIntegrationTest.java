package com.teggr.articluate.youtube;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class YoutubeTranscriptProviderIntegrationTest {

    @Test
    @EnabledIfSystemProperty(named = "youtube.integration", matches = "true")
    void fetchTranscriptFromLiveYoutubeUrl() {
        YoutubeTranscriptProvider provider = new YoutubeTranscriptProvider(new ObjectMapper());
        TranscriptResult result = provider.fetchTranscript("https://www.youtube.com/watch?v=nfIcjkR4KZ8");

        assertEquals("nfIcjkR4KZ8", result.videoId());
        assertNotNull(result.title());
        assertFalse(result.title().isBlank());
        assertNotNull(result.transcript());
        assertFalse(result.transcript().isBlank());
    }
}
