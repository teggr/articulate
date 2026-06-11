package com.teggr.articulate.articles.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teggr.articulate.articles.ArticleResponse;
import com.teggr.articulate.articles.ArticleService;
import com.teggr.articulate.transcripts.TranscriptNotFoundException;
import com.teggr.articulate.youtube.YouTubeVideoIdExtractor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(GenerateController.class)
class GenerateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

        @MockitoBean
        private YouTubeVideoIdExtractor videoIdExtractor;

    @Test
    void getGenerateReturnsHomeView() throws Exception {
        mockMvc.perform(get("/generate").with(user("test-user")))
                .andExpect(status().isOk())
                                .andExpect(view().name("generateArticleView"))
                                .andExpect(model().attributeExists("youtubeUrl"));
    }

        @Test
        void getGenerateWithUrlPrefillsYoutubeUrlField() throws Exception {
                mockMvc.perform(get("/generate")
                                                .with(user("test-user"))
                                                .param("url", "https://youtube.com/watch?v=abc1234DEFG"))
                                .andExpect(status().isOk())
                                .andExpect(view().name("generateArticleView"))
                                .andExpect(model().attribute("youtubeUrl", "https://youtube.com/watch?v=abc1234DEFG"));
        }

    @Test
    void postBlankUrlReturnsValidationError() throws Exception {
        mockMvc.perform(post("/generate")
                        .with(user("test-user"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("youtubeUrl", "   "))
                .andExpect(status().isOk())
                .andExpect(view().name("generateArticleView"))
                .andExpect(model().attribute("youtubeEmbedUrl", ""))
                .andExpect(model().attribute("error", "Please provide a YouTube URL."));
    }

    @Test
    void postHtmxEndpointReturnsRenderedHtml() throws Exception {
                when(videoIdExtractor.extract("https://www.youtube.com/watch?v=abc123")).thenReturn("abc1234DEFG");
        when(articleService.generate(any()))
                .thenReturn(new ArticleResponse("Title", "# Heading", "<h1>Heading</h1>"));

        mockMvc.perform(post("/generate")
                        .with(user("test-user"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("youtubeUrl", "https://www.youtube.com/watch?v=abc123"))
                .andExpect(status().isOk())
                .andExpect(view().name("generateArticleView"))
                .andExpect(model().attribute("youtubeEmbedUrl", "https://www.youtube-nocookie.com/embed/abc1234DEFG"))
                .andExpect(model().attribute("article", new ArticleResponse("Title", "# Heading", "<h1>Heading</h1>")));
    }

    @Test
    void postShowsRateLimitErrorFromTranscriptProvider() throws Exception {
        String rateLimitMessage = "YouTube is rate-limiting transcript requests right now. Please wait a few minutes and try again.";
                when(videoIdExtractor.extract("https://www.youtube.com/watch?v=nfIcjkR4KZ8")).thenReturn("nfIcjkR4KZ8");

        when(articleService.generate(any()))
                .thenThrow(new TranscriptNotFoundException(rateLimitMessage));

        mockMvc.perform(post("/generate")
                        .with(user("test-user"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("youtubeUrl", "https://www.youtube.com/watch?v=nfIcjkR4KZ8"))
                .andExpect(status().isOk())
                .andExpect(view().name("generateArticleView"))
                                .andExpect(model().attribute("youtubeEmbedUrl", "https://www.youtube-nocookie.com/embed/nfIcjkR4KZ8"))
                .andExpect(model().attribute("error", rateLimitMessage));
    }
}
