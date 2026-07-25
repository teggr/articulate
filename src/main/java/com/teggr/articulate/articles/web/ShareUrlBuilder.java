package com.teggr.articulate.articles.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

final class ShareUrlBuilder {

    private ShareUrlBuilder() {
    }

    static String build(HttpServletRequest request, String articleId) {
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/share/{articleId}")
                .replaceQuery(null)
                .buildAndExpand(articleId)
                .toUriString();
    }
}
