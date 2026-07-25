package com.teggr.articulate.articles.web.views;

import dev.rebelcraft.j2html.spring.webmvc.J2HtmlView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import j2html.tags.DomContent;
import org.springframework.stereotype.Component;

import com.teggr.articulate.articles.ArticleResponse;
import com.teggr.articulate.web.views.Page;

import java.util.Map;

import static j2html.TagCreator.a;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.iframe;
import static j2html.TagCreator.main;
import static j2html.TagCreator.p;
import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.script;
import static j2html.TagCreator.time;
import static j2html.TagCreator.text;

@Component("articleDetailView")
public class ArticleDetailView extends J2HtmlView {

    @Override
    protected DomContent renderMergedOutputModelDomContent(Map<String, Object> model,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        ArticleResponse article = (ArticleResponse) model.get("article");
        String createdAt = (String) model.getOrDefault("createdAt", "");
        String sourceUrl = (String) model.getOrDefault("sourceUrl", "");
        String youtubeEmbedUrl = (String) model.getOrDefault("youtubeEmbedUrl", "");
        String shareUrl = (String) model.getOrDefault("shareUrl", "");
        boolean publicShareView = (boolean) model.getOrDefault("publicShareView", false);

        DomContent sourceMeta = sourceUrl.isBlank()
                ? text("")
                : p().withClass("meta").with(
                        text("Source: "),
                        a(sourceUrl)
                                .withHref(sourceUrl)
                                .attr("target", "_blank")
                                .attr("rel", "noopener noreferrer")
                );

        DomContent videoSection = youtubeEmbedUrl.isBlank()
                ? text("")
                : div().withClass("section").with(
                        h2("Video"),
                        div().withClass("video-embed").with(
                                iframe()
                                        .withClass("video-frame")
                                        .attr("title", "YouTube video player")
                                        .withSrc(youtubeEmbedUrl)
                                        .attr("allow", "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share")
                                        .attr("allowfullscreen", "allowfullscreen")
                        )
                );

        DomContent shareSection = shareUrl.isBlank()
                ? text("")
                : div().withClass("section").with(
                        h2("Share"),
                        div().withClass("share-actions").with(
                                button("Share")
                                        .withType("button")
                                        .withClass("share-native")
                                        .attr("data-share-url", shareUrl),
                                button("Copy link")
                                        .withType("button")
                                        .withClass("share-copy")
                                        .attr("data-share-url", shareUrl)
                        )
                );

        return Page.render(
                article.title() + " | Articulate",
                "article-detail-page",
                main().withClass("page").with(
                        div().withClass("topbar").with(
                                publicShareView
                                        ? a("Articulate").withHref("/")
                                        : Page.primaryNav("/articles", request)
                        ),
                        h1(article.title()),
                        p().withClass("meta").with(
                                text("Created "),
                                time(createdAt).attr("datetime", createdAt)
                        ),
                        shareSection,
                        sourceMeta,
                        videoSection,
                        div().withClass("section").with(
                                h2("Rendered output"),
                                div(rawHtml(article.html()))
                        )
                ),
                script().withSrc("/share-actions.js").attr("defer", "defer")
        );
    }
}
