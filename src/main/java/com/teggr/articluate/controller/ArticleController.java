package com.teggr.articluate.controller;

import com.teggr.articluate.model.ArticleRequest;
import com.teggr.articluate.model.ArticleResponse;
import com.teggr.articluate.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * Converts a YouTube URL into a blog article.
     *
     * <p>Request body: {@code { "youtubeUrl": "https://youtube.com/watch?v=..." }}</p>
     * <p>Response: title, markdown, and rendered HTML.</p>
     */
    @PostMapping
    public ResponseEntity<ArticleResponse> generate(@Valid @RequestBody ArticleRequest request) {
        ArticleResponse response = articleService.generate(request);
        return ResponseEntity.ok(response);
    }
}
