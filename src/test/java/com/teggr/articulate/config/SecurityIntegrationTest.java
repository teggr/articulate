package com.teggr.articulate.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.teggr.articulate.articles.ArticleService;
import com.teggr.articulate.articles.web.ArticlesController;
import com.teggr.articulate.articles.web.GenerateController;
import com.teggr.articulate.web.LandingController;
import com.teggr.articulate.youtube.YouTubeVideoIdExtractor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({LandingController.class, GenerateController.class, ArticlesController.class})
@Import(SecurityConfig.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleService articleService;

    @MockitoBean
    private YouTubeVideoIdExtractor videoIdExtractor;

    @Test
    void landingPageIsPublic() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("landingView"))
                .andExpect(model().attribute("loginUrl", "/login"));
    }

    @Test
    void generatePageRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/generate"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));
    }

        @Test
        void generatePageWithUrlRequiresAuthenticationAndSavesTargetUrl() throws Exception {
        mockMvc.perform(get("/generate").param("url", "https://youtube.com/watch?v=abc1234DEFG"))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")))
            .andExpect(request().sessionAttribute(
                "SPRING_SECURITY_SAVED_REQUEST",
                org.hamcrest.Matchers.hasProperty(
                    "parameterMap",
                    org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.hasKey("url"),
                        org.hamcrest.Matchers.hasEntry(
                            org.hamcrest.Matchers.equalTo("url"),
                            org.hamcrest.Matchers.arrayContaining("https://youtube.com/watch?v=abc1234DEFG")
                        )
                    )
                )
            ));
        }

    @Test
    void generatePageIsAccessibleWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/generate").with(user("test-user")))
                .andExpect(status().isOk());
    }

    @Test
    void articlesPageRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/articles"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/login")));
    }
}
