package com.teggr.articulate.articles;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSystemArticleRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    private FileSystemArticleRepository repository() {
        return new FileSystemArticleRepository(tempDir.toString(), objectMapper);
    }

    @Test
    void findByIdReturnsEmptyWhenFileDoesNotExist() {
        Optional<ArticleResult> result = repository().findById("missing");
        assertTrue(result.isEmpty());
    }

    @Test
    void saveAndFindByIdRoundTrip() {
        ArticleResult original = new ArticleResult(
                "art-1234",
                "2026-06-10T22:00:00Z",
                "trn-1234",
                "Test Title",
                "# markdown");
        FileSystemArticleRepository repository = repository();

        repository.save(original);
        Optional<ArticleResult> loaded = repository.findById("art-1234");

        assertTrue(loaded.isPresent());
        assertEquals(original, loaded.get());
    }

    @Test
    void saveCreatesDirectoryIfAbsent() {
        Path dataDir = tempDir.resolve("nested");
        FileSystemArticleRepository repository = new FileSystemArticleRepository(dataDir.toString(), objectMapper);
        ArticleResult result = new ArticleResult(
                "art-1234",
                "2026-06-10T22:00:00Z",
                "trn-1234",
                "Title",
                "# markdown");

        repository.save(result);

        assertTrue(dataDir.resolve("articles").resolve("art-1234.json").toFile().exists());
    }

    @Test
    void findByTranscriptIdReturnsMostRecentArticle() {
        FileSystemArticleRepository repository = repository();
        ArticleResult older = new ArticleResult(
                "art-old",
                "2026-06-10T22:00:00Z",
                "trn-1234",
                "Older",
                "# older");
        ArticleResult newer = new ArticleResult(
                "art-new",
                "2026-06-11T10:00:00Z",
                "trn-1234",
                "Newer",
                "# newer");

        repository.save(older);
        repository.save(newer);

        Optional<ArticleResult> loaded = repository.findByTranscriptId("trn-1234");

        assertTrue(loaded.isPresent());
        assertEquals(newer, loaded.get());
    }
}
