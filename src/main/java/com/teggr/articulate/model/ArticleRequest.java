package com.teggr.articulate.model;

import jakarta.validation.constraints.NotBlank;

public record ArticleRequest(
        @NotBlank(message = "youtubeUrl must not be blank")
        String youtubeUrl
) {
}
