package com.teggr.articulate.youtube;

import org.junit.jupiter.api.Test;

import com.teggr.articulate.youtube.YouTubeVideoIdExtractor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YouTubeVideoIdExtractorTest {

    private final YouTubeVideoIdExtractor extractor = new YouTubeVideoIdExtractor();

    @Test
    void extractVideoIdFromWatchUrl() {
        assertEquals("nfIcjkR4KZ8", extractor.extract("https://www.youtube.com/watch?v=nfIcjkR4KZ8"));
    }

    @Test
    void extractVideoIdFromShortUrl() {
        assertEquals("nfIcjkR4KZ8", extractor.extract("https://youtu.be/nfIcjkR4KZ8"));
    }

    @Test
    void extractVideoIdFromShortsUrl() {
        assertEquals("nfIcjkR4KZ8", extractor.extract("https://www.youtube.com/shorts/nfIcjkR4KZ8"));
    }

    @Test
    void extractVideoIdFromEmbedUrl() {
        assertEquals("nfIcjkR4KZ8", extractor.extract("https://www.youtube.com/embed/nfIcjkR4KZ8"));
    }

    @Test
    void extractVideoIdFromBareId() {
        assertEquals("nfIcjkR4KZ8", extractor.extract("nfIcjkR4KZ8"));
    }

    @Test
    void rejectInvalidUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> extractor.extract("https://www.youtube.com/watch?v=invalid"));
    }
}
