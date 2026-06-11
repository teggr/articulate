package com.teggr.articulate.service.transcripts;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.teggr.articulate.service.transcripts.FileSystemTranscriptRepository;
import com.teggr.articulate.service.transcripts.TranscriptResult;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemTranscriptRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    private FileSystemTranscriptRepository repository() {
        return new FileSystemTranscriptRepository(tempDir.toString(), objectMapper);
    }

    @Test
    void findByIdReturnsEmptyWhenFileDoesNotExist() {
        Optional<TranscriptResult> result = repository().findById("missing");
        assertTrue(result.isEmpty());
    }

    @Test
    void saveAndFindByIdRoundTrip() {
        TranscriptResult original = new TranscriptResult(
                "trn-1234",
                "2026-06-10T22:00:00Z",
                "abc1234DEFG",
                "Test Title",
                "This is the transcript.");
        FileSystemTranscriptRepository repository = repository();

        repository.save(original);
        Optional<TranscriptResult> loaded = repository.findById("trn-1234");

        assertTrue(loaded.isPresent());
        assertEquals(original, loaded.get());
    }

    @Test
    void saveCreatesDirectoryIfAbsent() {
        Path subDir = tempDir.resolve("nested").resolve("transcripts");
        FileSystemTranscriptRepository repository = new FileSystemTranscriptRepository(subDir.toString(), objectMapper);
        TranscriptResult result = new TranscriptResult(
                "trn-1234",
                "2026-06-10T22:00:00Z",
                "abc1234DEFG",
                "Title",
                "Transcript");

        repository.save(result);

        assertTrue(subDir.resolve("trn-1234.json").toFile().exists());
    }
}
