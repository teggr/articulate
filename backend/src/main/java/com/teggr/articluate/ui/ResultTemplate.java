package com.teggr.articluate.ui;

import com.teggr.articluate.model.ArticleResponse;
import j2html.tags.DomContent;
import org.springframework.stereotype.Component;
import sh.rebelstack.j2html.engine.HtmlComponent;
import sh.rebelstack.j2html.engine.HtmlTemplate;
import sh.rebelstack.j2html.engine.RenderContext;

import java.util.Optional;

import static j2html.TagCreator.article;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.p;
import static j2html.TagCreator.pre;
import static j2html.TagCreator.unsafeHtml;

@Component
@HtmlTemplate("ui/result")
public class ResultTemplate implements HtmlComponent {

    @Override
    public DomContent render(RenderContext ctx) {
        Optional<String> error = ctx.find("error", String.class)
                .filter(message -> !message.isBlank());
        if (error.isPresent()) {
            return div(
                    h3("Unable to generate article"),
                    p(error.get())
            );
        }

        Optional<ArticleResponse> articleResponse = ctx.find("article", ArticleResponse.class);
        if (articleResponse.isEmpty()) {
            return div(p("No article yet."));
        }

        ArticleResponse article = articleResponse.get();
        return article().with(
                h2(article.title()),
                h3("Rendered output"),
                div(unsafeHtml(article.html())),
                h3("Markdown"),
                pre(article.markdown())
        );
    }
}
