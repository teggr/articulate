package com.teggr.articulate.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.teggr.articulate.exception.TranscriptNotFoundException;
import com.teggr.articulate.model.ArticleRequest;
import com.teggr.articulate.model.ArticleResponse;
import com.teggr.articulate.service.ArticleService;

@Controller
@RequestMapping("/generate")
@RequiredArgsConstructor
public class GenerateController {

    private static final Logger log = LoggerFactory.getLogger(GenerateController.class);
    private static final String EMPTY_URL_ERROR = "Please provide a YouTube URL.";
    private static final String GENERIC_ERROR = "Unable to generate article right now. Please try again.";

    private final ArticleService articleService;

    @GetMapping
    public String index(Model model) {
        if (!model.containsAttribute("youtubeUrl")) {
            model.addAttribute("youtubeUrl", "");
        }
        return "generateArticleView";
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String generate(@RequestParam(name = "youtubeUrl", required = false) String youtubeUrl, Model model) {
        String normalizedUrl = normalize(youtubeUrl);
        model.addAttribute("youtubeUrl", normalizedUrl);
        applyGeneration(model, normalizedUrl);
        return "generateArticleView";
    }

    private void applyGeneration(Model model, String normalizedUrl) {
        model.addAttribute("article", null);
        model.addAttribute("error", null);

        if (normalizedUrl.isBlank()) {
            model.addAttribute("error", EMPTY_URL_ERROR);
            return;
        }

        GenerationResult result = generateArticle(normalizedUrl);
        model.addAttribute("article", result.article());
        model.addAttribute("error", result.error());
    }

    private GenerationResult generateArticle(String normalizedUrl) {
        try {
            return new GenerationResult(articleService.generate(new ArticleRequest(normalizedUrl)), null);
        } catch (IllegalArgumentException | TranscriptNotFoundException ex) {
            return new GenerationResult(null, ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unexpected UI generation error", ex);
            return new GenerationResult(null, GENERIC_ERROR);
        }
    }

    private String normalize(String youtubeUrl) {
        return youtubeUrl == null ? "" : youtubeUrl.trim();
    }

    private record GenerationResult(ArticleResponse article, String error) {
    }
}
