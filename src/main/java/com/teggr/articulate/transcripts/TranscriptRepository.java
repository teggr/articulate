package com.teggr.articulate.transcripts;

import java.util.Optional;

public interface TranscriptRepository {

    Optional<TranscriptResult> findById(String id);

    Optional<TranscriptResult> findByVideoId(String videoId);

    void save(TranscriptResult transcript);
}
