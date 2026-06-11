package com.teggr.articulate.service.transcripts;

import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
@Slf4j
public class FileSystemTranscriptRepository implements TranscriptRepository {

    private final Path repositoryDir;
    private final ObjectMapper objectMapper;

    public FileSystemTranscriptRepository(
            @Value("${articulate.data.dir:.data}") String dataDirPath,
            ObjectMapper objectMapper) {
        this.repositoryDir = Path.of(dataDirPath, "transcripts");
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<TranscriptResult> findById(String id) {
        Path file = transcriptFile(id);
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(file.toFile(), TranscriptResult.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to read transcript " + id, e);
        }
    }

    @Override
    public void save(TranscriptResult transcript) {
        if (transcript.id() == null || transcript.id().isBlank()) {
            throw new IllegalArgumentException("Transcript id is required before saving");
        }

        try {
            Files.createDirectories(repositoryDir);
            Path file = transcriptFile(transcript.id());
            objectMapper.writeValue(file.toFile(), transcript);
            log.debug("Stored transcript {} -> {}", transcript.id(), file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save transcript " + transcript.id(), e);
        }
    }

    private Path transcriptFile(String id) {
        return repositoryDir.resolve(id + ".json");
    }
}
