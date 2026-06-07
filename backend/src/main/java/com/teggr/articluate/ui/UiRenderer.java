package com.teggr.articluate.ui;

import com.teggr.articluate.model.ArticleResponse;
import j2html.tags.DomContent;
import j2html.tags.specialized.DivTag;

import static j2html.TagCreator.article;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.p;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.rawHtml;

public final class UiRenderer {

    private UiRenderer() {
    }

    public static DivTag resultContainer(ArticleResponse article, String error) {
        return div(resultContent(article, error)).withId("article-result");
    }

    public static DomContent resultContent(ArticleResponse article, String error) {
        if (error != null && !error.isBlank()) {
            return div(
                    h3("Unable to generate article"),
                    p(error)
            );
        }

        if (article == null) {
            return div(p("No article yet."));
        }

        return article().with(
                h2(article.title()),
                h3("Rendered output"),
                div(rawHtml(article.html())),
                h3("Markdown"),
                pre(article.markdown())
        );
    }

    public static String renderResultContent(ArticleResponse article, String error) {
        return resultContent(article, error).render();
    }
}
