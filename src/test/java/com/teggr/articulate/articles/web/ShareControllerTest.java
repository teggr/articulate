package com.teggr.articulate.articles.web;

import com.teggr.articulate.articles.ArticleResponse;
import com.teggr.articulate.articles.ArticleResult;
import com.teggr.articulate.articles.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ShareController.class)
class ShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @Test
    void getSharedArticleIsPublicAndReturnsDetailView() throws Exception {
        ArticleResult stored = new ArticleResult("a2", "2026-06-11T10:00:00Z", "t2", "Newest", "# newest");
        ArticleResponse response = new ArticleResponse("Newest", "# newest", "<h1>newest</h1>");
        when(articleService.findById("a2")).thenReturn(Optional.of(stored));
        when(articleService.findResponseById("a2")).thenReturn(Optional.of(response));
        when(articleService.findSourceUrlByTranscriptId("t2"))
                .thenReturn(Optional.of("https://www.youtube.com/watch?v=abc123"));
        when(articleService.findYoutubeEmbedUrlByTranscriptId("t2"))
                .thenReturn(Optional.of("https://www.youtube-nocookie.com/embed/abc123"));

        mockMvc.perform(get("/share/a2"))
                .andExpect(status().isOk())
                .andExpect(view().name("articleDetailView"))
                .andExpect(model().attribute("article", response))
                .andExpect(model().attribute("articleId", "a2"))
                .andExpect(model().attribute("createdAt", "2026-06-11T10:00:00Z"))
                .andExpect(model().attribute("sourceUrl", "https://www.youtube.com/watch?v=abc123"))
                .andExpect(model().attribute("youtubeEmbedUrl", "https://www.youtube-nocookie.com/embed/abc123"))
                .andExpect(model().attribute("shareUrl", "http://localhost/share/a2"))
                .andExpect(model().attribute("publicShareView", true));
    }

    @Test
    void getSharedArticleReturns404WhenMissing() throws Exception {
        when(articleService.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/share/missing"))
                .andExpect(status().isNotFound());
    }
}
