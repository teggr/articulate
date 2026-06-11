package com.teggr.articulate.articles;

public record ArticleResult(
        String id,
        String createdAt,
        String transcriptId,
        String title,
        String markdown
) {
}
