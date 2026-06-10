package com.teggr.articulate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teggr.articulate.controller.GenerateController;
import com.teggr.articulate.exception.TranscriptNotFoundException;
import com.teggr.articulate.model.ArticleResponse;
import com.teggr.articulate.service.ArticleService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(GenerateController.class)
class GenerateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @Test
    void getGenerateReturnsHomeView() throws Exception {
        mockMvc.perform(get("/generate").with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(view().name("homeView"))
                .andExpect(model().attributeExists("youtubeUrl"));
    }

    @Test
    void postBlankUrlReturnsValidationError() throws Exception {
        mockMvc.perform(post("/generate")
                        .with(user("test-user"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("youtubeUrl", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("homeView"))
                .andExpect(model().attribute("error", "Please provide a YouTube URL."));
    }

    @Test
    void postHtmxEndpointReturnsRenderedHtml() throws Exception {
        when(articleService.generate(any()))
                .thenReturn(new ArticleResponse("Title", "# Heading", "<h1>Heading</h1>"));

        mockMvc.perform(post("/generate/ui/articles")
                        .with(user("test-user"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("youtubeUrl", "https://www.youtube.com/watch?v=abc123"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Rendered output")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Heading")));
    }

    @Test
    void postShowsRateLimitErrorFromTranscriptProvider() throws Exception {
        String rateLimitMessage = "YouTube is rate-limiting transcript requests right now. Please wait a few minutes and try again.";

        when(articleService.generate(any()))
                .thenThrow(new TranscriptNotFoundException(rateLimitMessage));

        mockMvc.perform(post("/generate")
                        .with(user("test-user"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("youtubeUrl", "https://www.youtube.com/watch?v=nfIcjkR4KZ8"))
                .andExpect(status().isOk())
                .andExpect(view().name("homeView"))
                .andExpect(model().attribute("error", rateLimitMessage));
    }
}
