package com.teggr.articulate.articles.web.views;

import dev.rebelcraft.j2html.spring.webmvc.J2HtmlView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import j2html.tags.DomContent;
import org.springframework.stereotype.Component;

import com.teggr.articulate.articles.ArticleResult;
import com.teggr.articulate.web.views.Page;
import j2html.tags.specialized.PTag;

import java.util.List;
import java.util.Map;

import static j2html.TagCreator.a;
import static j2html.TagCreator.article;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.main;
import static j2html.TagCreator.p;
import static j2html.TagCreator.script;
import static j2html.TagCreator.time;
import static j2html.TagCreator.text;

@Component("articlesView")
public class ArticlesView extends J2HtmlView {

    @Override
    @SuppressWarnings("unchecked")
    protected DomContent renderMergedOutputModelDomContent(Map<String, Object> model,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        List<ArticleResult> articles = (List<ArticleResult>) model.getOrDefault("articles", List.of());
        Map<String, String> sourceUrls = (Map<String, String>) model.getOrDefault("sourceUrls", Map.of());
        Map<String, String> shareUrls = (Map<String, String>) model.getOrDefault("shareUrls", Map.of());

        return Page.render(
                "Articles | Articulate",
                "articles-page",
                main().withClass("page").with(
                        div().withClass("topbar").with(
                                h1("Your Articles"),
                                Page.primaryNav("/articles", request)
                        ),
                        p("Newest articles appear first.").withClass("intro"),
                        articles.isEmpty()
                                ? div("No articles have been created yet.").withClass("empty")
                                : div(articles.stream()
                                        .map(article -> articleCard(article, sourceUrls, shareUrls))
                                        .toArray(DomContent[]::new)).withClass("list")
                ),
                script().withSrc("/share-actions.js").attr("defer", "defer")
        );
    }

    private DomContent articleCard(ArticleResult article, Map<String, String> sourceUrls, Map<String, String> shareUrls) {
        String sourceUrl = sourceUrls.get(article.id());
        String shareUrl = shareUrls.getOrDefault(article.id(), "");
        PTag meta = p().withClass("meta").with(
                text("Created "),
                time(article.createdAt()).attr("datetime", article.createdAt())
        );
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            meta.with(
                    text(" | "),
                                        a(sourceUrl)
                            .withHref(sourceUrl)
                            .attr("target", "_blank")
                            .attr("rel", "noopener noreferrer")
            );
        }

        return article().withClass("card").with(
                h2(a(article.title()).withHref("/articles/" + article.id())),
                meta,
                shareUrl.isBlank()
                        ? text("")
                        : div().withClass("share-actions").with(
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
    }
}
