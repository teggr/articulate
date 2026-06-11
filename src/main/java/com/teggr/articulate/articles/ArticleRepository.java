package com.teggr.articulate.articles;

import java.util.Optional;

public interface ArticleRepository {

    Optional<ArticleResult> findById(String id);

    Optional<ArticleResult> findByTranscriptId(String transcriptId);

    void save(ArticleResult article);
}
