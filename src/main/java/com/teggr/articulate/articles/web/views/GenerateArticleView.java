package com.teggr.articulate.articles.web.views;

import dev.rebelcraft.j2html.spring.webmvc.J2HtmlView;
import j2html.tags.DomContent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import com.teggr.articulate.articles.ArticleResponse;
import com.teggr.articulate.web.views.Page;

import org.springframework.security.web.csrf.CsrfToken;

import java.util.Map;

import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxIndicator;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxPost;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxSelect;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxSwap;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxTarget;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.innerHTML;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.main;
import static j2html.TagCreator.p;
import static j2html.TagCreator.script;

@Component
public class GenerateArticleView extends J2HtmlView {

    @Override
    protected DomContent renderMergedOutputModelDomContent(Map<String, Object> model,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        String youtubeUrl = (String) model.getOrDefault("youtubeUrl", "");
        String youtubeEmbedUrl = (String) model.getOrDefault("youtubeEmbedUrl", "");
        ArticleResponse article = (ArticleResponse) model.get("article");
        String error = (String) model.get("error");
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        return Page.render(
                "Articulate",
                "generate-page",
                main().withClass("container").with(
                        div().withClass("topbar").with(
                                h1("YouTube to Blog"),
                                Page.primaryNav("/generate", request)
                        ),
                        p("Submit a YouTube URL to generate an article."),
                        form().withMethod("post")
                                .withAction("/generate")
                                .attr(hxPost("/generate"))
                                .attr(hxTarget("#article-result"))
                                .attr(hxSelect("#article-result"))
                                .attr(hxSwap(innerHTML))
                                .attr(hxIndicator("#loading-indicator"))
                                .with(
                                        label("YouTube URL").withFor("youtube-url"),
                                        input().withId("youtube-url")
                                                .withName("youtubeUrl")
                                                .withType("url")
                                                .withPlaceholder("https://www.youtube.com/watch?v=...")
                                                .withValue(youtubeUrl)
                                                .isRequired(),
                                        input().withType("hidden")
                                                .withName(csrfToken != null ? csrfToken.getParameterName() : "_csrf")
                                                .withValue(csrfToken != null ? csrfToken.getToken() : ""),
                                        button("Generate article").withType("submit"),
                                        div().withId("loading-indicator").withClass("htmx-indicator loading-indicator").with(
                                                div().withClass("spinner"),
                                                p("Generating your article... this may take a moment")
                                        )
                                ),
                                                UiRenderer.resultContainer(article, error, youtubeEmbedUrl)
                ),
                script().withSrc("https://unpkg.com/htmx.org@2.0.4")
        );
    }
}
