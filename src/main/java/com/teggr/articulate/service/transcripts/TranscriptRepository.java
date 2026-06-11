package com.teggr.articulate.service.transcripts;

import java.util.Optional;

public interface TranscriptRepository {

    Optional<TranscriptResult> findById(String id);

    void save(TranscriptResult transcript);
}
