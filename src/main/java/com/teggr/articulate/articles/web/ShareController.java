package com.teggr.articulate.articles.web;

import com.teggr.articulate.articles.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class ShareController {

    private final ArticleService articleService;

    @GetMapping("/share/{articleId}")
    public String show(@PathVariable String articleId, Model model, HttpServletRequest request) {
        var article = articleService.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found"));
        model.addAttribute("article", articleService.findResponseById(articleId).orElseThrow());
        model.addAttribute("articleId", article.id());
        model.addAttribute("createdAt", article.createdAt());
        model.addAttribute("sourceUrl", articleService.findSourceUrlByTranscriptId(article.transcriptId()).orElse(""));
        model.addAttribute("youtubeEmbedUrl", articleService.findYoutubeEmbedUrlByTranscriptId(article.transcriptId()).orElse(""));
        model.addAttribute("shareUrl", ShareUrlBuilder.build(request, article.id()));
        model.addAttribute("publicShareView", true);
        return "articleDetailView";
    }
}
