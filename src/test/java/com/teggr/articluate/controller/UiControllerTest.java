package com.teggr.articluate.controller;

import com.teggr.articluate.model.ArticleResponse;
import com.teggr.articluate.service.ArticleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UiController.class)
class UiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArticleService articleService;

    @Test
    void getIndexReturnsIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("homeView"))
                .andExpect(model().attributeExists("youtubeUrl"));
    }

    @Test
    void postBlankUrlReturnsValidationError() throws Exception {
        mockMvc.perform(post("/")
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

        mockMvc.perform(post("/ui/articles")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("youtubeUrl", "https://www.youtube.com/watch?v=abc123"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Rendered output")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("Heading")));
    }
}
