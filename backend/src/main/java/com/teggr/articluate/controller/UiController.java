package com.teggr.articluate.controller;

import com.teggr.articluate.model.ArticleRequest;
import com.teggr.articluate.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class UiController {

    private final ArticleService articleService;

    @GetMapping("/")
    public String index(Model model) {
        if (!model.containsAttribute("youtubeUrl")) {
            model.addAttribute("youtubeUrl", "");
        }
        return "ui/index";
    }

    @PostMapping(value = "/", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String generate(@RequestParam(name = "youtubeUrl", required = false) String youtubeUrl,
                           @RequestHeader(value = "HX-Request", required = false) String htmxRequest,
                           Model model) {
        String normalizedUrl = youtubeUrl == null ? "" : youtubeUrl.trim();
        model.addAttribute("youtubeUrl", normalizedUrl);

        if (normalizedUrl.isBlank()) {
            model.addAttribute("error", "Please provide a YouTube URL.");
            return resolveView(htmxRequest);
        }

        try {
            model.addAttribute("article", articleService.generate(new ArticleRequest(normalizedUrl)));
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return resolveView(htmxRequest);
    }

    private String resolveView(String htmxRequest) {
        return "true".equalsIgnoreCase(htmxRequest) ? "ui/result" : "ui/index";
    }
}
