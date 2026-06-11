package com.teggr.articulate.articles;

import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@Slf4j
public class FileSystemArticleRepository implements ArticleRepository {

    private final Path repositoryDir;
    private final ObjectMapper objectMapper;

    public FileSystemArticleRepository(
            @Value("${articulate.data.dir:.data}") String dataDirPath,
            ObjectMapper objectMapper) {
        this.repositoryDir = Path.of(dataDirPath, "articles");
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ArticleResult> findById(String id) {
        Path file = articleFile(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(file.toFile(), ArticleResult.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read article " + id, e);
        }
    }

    @Override
    public Optional<ArticleResult> findByTranscriptId(String transcriptId) {
        if (!Files.exists(repositoryDir)) {
            return Optional.empty();
        }

        try (Stream<Path> files = Files.list(repositoryDir)) {
            return files
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .map(this::readArticleSafely)
                    .flatMap(Optional::stream)
                    .filter(article -> transcriptId.equals(article.transcriptId()))
                    .sorted(Comparator.comparing(ArticleResult::createdAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .findFirst();
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan articles for transcript id " + transcriptId, e);
        }
    }

    @Override
    public void save(ArticleResult article) {
        if (article.id() == null || article.id().isBlank()) {
            throw new IllegalArgumentException("Article id is required before saving");
        }

        try {
            Files.createDirectories(repositoryDir);
            Path file = articleFile(article.id());
            objectMapper.writeValue(file.toFile(), article);
            log.debug("Stored article {} -> {}", article.id(), file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save article " + article.id(), e);
        }
    }

    private Path articleFile(String id) {
        return repositoryDir.resolve(id + ".json");
    }

    private Optional<ArticleResult> readArticleSafely(Path file) {
        try {
            return Optional.of(objectMapper.readValue(file.toFile(), ArticleResult.class));
        } catch (Exception e) {
            log.warn("Skipping unreadable article file {}", file, e);
            return Optional.empty();
        }
    }
}
