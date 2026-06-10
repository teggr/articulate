package com.teggr.articulate.model;

public record ArticleResponse(
        String title,
        String markdown,
        String html
) {
}
