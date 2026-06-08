package com.teggr.articluate.model;

public record ArticleResponse(
        String title,
        String markdown,
        String html
) {
}
