package com.teggr.articluate.controller;

import com.teggr.articluate.exception.TranscriptNotFoundException;
import com.teggr.articluate.model.ArticleRequest;
import com.teggr.articluate.model.ArticleResponse;
import com.teggr.articluate.service.ArticleService;
import com.teggr.articluate.ui.UiRenderer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class UiController {

    private static final Logger log = LoggerFactory.getLogger(UiController.class);
    private static final String EMPTY_URL_ERROR = "Please provide a YouTube URL.";
    private static final String GENERIC_ERROR = "Unable to generate article right now. Please try again.";

    private final ArticleService articleService;

    @GetMapping("/")
    public String index(Model model) {
        if (!model.containsAttribute("youtubeUrl")) {
            model.addAttribute("youtubeUrl", "");
        }
        return "homeView";
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String generate(@RequestParam(name = "youtubeUrl", required = false) String youtubeUrl, Model model) {
        String normalizedUrl = normalize(youtubeUrl);
        model.addAttribute("youtubeUrl", normalizedUrl);
        applyGeneration(model, normalizedUrl);
        return "homeView";
    }

    @PostMapping(value = "/ui/articles", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String generatePartial(@RequestParam(name = "youtubeUrl", required = false) String youtubeUrl) {
        String normalizedUrl = normalize(youtubeUrl);
        if (normalizedUrl.isBlank()) {
            return UiRenderer.renderResultContent(null, EMPTY_URL_ERROR);
        }

        GenerationResult result = generateArticle(normalizedUrl);
        return UiRenderer.renderResultContent(result.article(), result.error());
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
