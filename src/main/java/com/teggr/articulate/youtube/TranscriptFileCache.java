package com.teggr.articulate.youtube;

import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads and writes {@link TranscriptResult} JSON files under a configurable local directory.
 * File names are {@code {videoId}.json}.
 */
@Component
@Slf4j
public class TranscriptFileCache {

    private final Path cacheDir;
    private final ObjectMapper objectMapper;

    public TranscriptFileCache(
            @Value("${articulate.transcript-cache.dir:.data}") String cacheDirPath,
            ObjectMapper objectMapper) {
        this.cacheDir = Path.of(cacheDirPath);
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the cached {@link TranscriptResult} for {@code videoId}, or empty if not cached.
     */
    public Optional<TranscriptResult> load(String videoId) {
        Path file = cacheFile(videoId);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            TranscriptResult result = objectMapper.readValue(file.toFile(), TranscriptResult.class);
            log.debug("Cache hit for video ID: {}", videoId);
            return Optional.of(result);
          } catch (Exception e) {
            log.warn("Failed to read cached transcript for {}: {}", videoId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Persists {@code result} to disk as {@code {videoId}.json}.
     * Creates the cache directory if it does not exist.
     */
    public void save(TranscriptResult result) {
        try {
            Files.createDirectories(cacheDir);
            Path file = cacheFile(result.videoId());
            objectMapper.writeValue(file.toFile(), result);
            log.debug("Cached transcript for video ID: {} → {}", result.videoId(), file);
          } catch (Exception e) {
            log.warn("Failed to cache transcript for {}: {}", result.videoId(), e.getMessage());
        }
    }

    private Path cacheFile(String videoId) {
        return cacheDir.resolve(videoId + ".json");
    }
}
