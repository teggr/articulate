package com.teggr.articulate.articles.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teggr.articulate.articles.ArticleResult;
import com.teggr.articulate.articles.ArticleResponse;
import com.teggr.articulate.articles.ArticleService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ArticlesController.class)
class ArticlesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @Test
    void getArticlesReturnsArticlesView() throws Exception {
        List<ArticleResult> articles = List.of(
                new ArticleResult("a2", "2026-06-11T10:00:00Z", "t2", "Newest", "# newest"),
                new ArticleResult("a1", "2026-06-10T10:00:00Z", "t1", "Older", "# older")
        );
        when(articleService.findAll()).thenReturn(articles);
        when(articleService.findSourceUrlByTranscriptId("t2"))
            .thenReturn(Optional.of("https://www.youtube.com/watch?v=abc123"));
        when(articleService.findSourceUrlByTranscriptId("t1"))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/articles").with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(view().name("articlesView"))
            .andExpect(model().attribute("articles", articles))
            .andExpect(model().attribute("sourceUrls", Map.of("a2", "https://www.youtube.com/watch?v=abc123")))
            .andExpect(model().attribute("shareUrls", Map.of(
                    "a2", "http://localhost/share/a2",
                    "a1", "http://localhost/share/a1"
            )));
    }

    @Test
    void getArticleDetailReturnsDetailView() throws Exception {
        ArticleResult stored = new ArticleResult("a2", "2026-06-11T10:00:00Z", "t2", "Newest", "# newest");
        ArticleResponse response = new ArticleResponse("Newest", "# newest", "<h1>newest</h1>");
        when(articleService.findById("a2")).thenReturn(Optional.of(stored));
        when(articleService.findResponseById("a2")).thenReturn(Optional.of(response));
        when(articleService.findSourceUrlByTranscriptId("t2"))
            .thenReturn(Optional.of("https://www.youtube.com/watch?v=abc123"));
        when(articleService.findYoutubeEmbedUrlByTranscriptId("t2"))
            .thenReturn(Optional.of("https://www.youtube-nocookie.com/embed/abc123"));

        mockMvc.perform(get("/articles/a2").with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(view().name("articleDetailView"))
                .andExpect(model().attribute("article", response))
                .andExpect(model().attribute("articleId", "a2"))
            .andExpect(model().attribute("createdAt", "2026-06-11T10:00:00Z"))
            .andExpect(model().attribute("sourceUrl", "https://www.youtube.com/watch?v=abc123"))
            .andExpect(model().attribute("youtubeEmbedUrl", "https://www.youtube-nocookie.com/embed/abc123"))
            .andExpect(model().attribute("shareUrl", "http://localhost/share/a2"))
            .andExpect(model().attribute("publicShareView", false))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Source: ")))
            .andExpect(result -> {
                String body = result.getResponse().getContentAsString();
                int videoIndex = body.indexOf(">Video<");
                int sourceIndex = body.indexOf("Source:");
                assertTrue(videoIndex >= 0, "Expected video heading in rendered response");
                assertTrue(sourceIndex > videoIndex, "Expected source metadata to render below the video heading");
            });
    }

    @Test
    void getArticleDetailShowsSourceWhenVideoMissing() throws Exception {
        ArticleResult stored = new ArticleResult("a2", "2026-06-11T10:00:00Z", "t2", "Newest", "# newest");
        ArticleResponse response = new ArticleResponse("Newest", "# newest", "<h1>newest</h1>");
        when(articleService.findById("a2")).thenReturn(Optional.of(stored));
        when(articleService.findResponseById("a2")).thenReturn(Optional.of(response));
        when(articleService.findSourceUrlByTranscriptId("t2"))
            .thenReturn(Optional.of("https://example.com/source"));
        when(articleService.findYoutubeEmbedUrlByTranscriptId("t2"))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/articles/a2").with(user("test-user")))
                .andExpect(status().isOk())
                .andExpect(view().name("articleDetailView"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Source: ")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(">Video<"))));
    }

    @Test
    void getArticleDetailReturns404WhenMissing() throws Exception {
        when(articleService.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/articles/missing").with(user("test-user")))
                .andExpect(status().isNotFound());
    }
}
