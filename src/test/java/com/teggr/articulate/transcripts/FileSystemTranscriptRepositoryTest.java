package com.teggr.articulate.transcripts;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        Path dataDir = tempDir.resolve("nested");
        FileSystemTranscriptRepository repository = new FileSystemTranscriptRepository(dataDir.toString(), objectMapper);
        TranscriptResult result = new TranscriptResult(
                "trn-1234",
                "2026-06-10T22:00:00Z",
                "abc1234DEFG",
                "Title",
                "Transcript");

        repository.save(result);

        assertTrue(dataDir.resolve("transcripts").resolve("trn-1234.json").toFile().exists());
    }

    @Test
    void findByVideoIdReturnsMostRecentTranscript() {
        FileSystemTranscriptRepository repository = repository();
        TranscriptResult older = new TranscriptResult(
                "trn-old",
                "2026-06-10T22:00:00Z",
                "abc1234DEFG",
                "Older",
                "Older transcript");
        TranscriptResult newer = new TranscriptResult(
                "trn-new",
                "2026-06-11T10:00:00Z",
                "abc1234DEFG",
                "Newer",
                "Newer transcript");

        repository.save(older);
        repository.save(newer);

        Optional<TranscriptResult> loaded = repository.findByVideoId("abc1234DEFG");

        assertTrue(loaded.isPresent());
        assertEquals(newer, loaded.get());
    }
}
