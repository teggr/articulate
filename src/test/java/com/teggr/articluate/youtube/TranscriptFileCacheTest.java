package com.teggr.articluate.youtube;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscriptFileCacheTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    private TranscriptFileCache cache() {
        return new TranscriptFileCache(tempDir.toString(), objectMapper);
    }

    @Test
    void loadReturnsEmptyWhenFileDoesNotExist() {
        Optional<TranscriptResult> result = cache().load("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void saveAndLoadRoundTrip() {
        TranscriptResult original = new TranscriptResult("abc1234DEFG", "Test Title", "This is the transcript.");
        TranscriptFileCache cache = cache();

        cache.save(original);
        Optional<TranscriptResult> loaded = cache.load("abc1234DEFG");

        assertTrue(loaded.isPresent());
        assertEquals(original, loaded.get());
    }

    @Test
    void saveCreatesDirectoryIfAbsent() {
        Path subDir = tempDir.resolve("nested").resolve("cache");
        TranscriptFileCache cache = new TranscriptFileCache(subDir.toString(), objectMapper);
        TranscriptResult result = new TranscriptResult("abc1234DEFG", "Title", "Transcript");

        cache.save(result);

        assertTrue(subDir.resolve("abc1234DEFG.json").toFile().exists());
    }
}
