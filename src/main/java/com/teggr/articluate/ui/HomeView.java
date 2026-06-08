package com.teggr.articluate.ui;

import com.teggr.articluate.model.ArticleResponse;
import dev.rebelcraft.j2html.spring.webmvc.J2HtmlView;
import j2html.tags.DomContent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxIndicator;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxPost;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxSwap;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.hxTarget;
import static dev.rebelcraft.j2html.htmx.HtmxAttributes.innerHTML;
import static j2html.TagCreator.body;
import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.main;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.p;
import static j2html.TagCreator.script;
import static j2html.TagCreator.style;
import static j2html.TagCreator.title;

@Component("homeView")
public class HomeView extends J2HtmlView {

    @Override
    protected DomContent renderMergedOutputModelDomContent(Map<String, Object> model,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        String youtubeUrl = (String) model.getOrDefault("youtubeUrl", "");
        ArticleResponse article = (ArticleResponse) model.get("article");
        String error = (String) model.get("error");

        return html(
                head(
                        meta().withCharset("UTF-8"),
                        meta().attr("name", "viewport").attr("content", "width=device-width, initial-scale=1"),
                        title("Articluate"),
                        script().withSrc("https://unpkg.com/htmx.org@2.0.4"),
                        style("""
                                body { font-family: system-ui, sans-serif; margin: 0; background: #f7f7f7; color: #222; }
                                .container { max-width: 960px; margin: 0 auto; padding: 2rem 1rem; }
                                form { display: grid; gap: 0.75rem; background: #fff; padding: 1rem; border-radius: 8px; }
                                input, button { font-size: 1rem; padding: 0.65rem 0.75rem; }
                                button { width: fit-content; cursor: pointer; }
                                .htmx-indicator { display: none; }
                                .htmx-request .htmx-indicator { display: inline; }
                                #article-result { margin-top: 1.5rem; background: #fff; padding: 1rem; border-radius: 8px; }
                                pre { white-space: pre-wrap; overflow-wrap: anywhere; background: #f2f2f2; padding: 0.75rem; border-radius: 6px; }
                                """)
                ),
                body(
                        main().withClass("container").with(
                                h1("YouTube to Blog"),
                                p("Submit a YouTube URL to generate an article."),
                                form().withMethod("post")
                                        .withAction("/")
                                        .attr(hxPost("/ui/articles"))
                                        .attr(hxTarget("#article-result"))
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
                                                button("Generate article").withType("submit"),
                                                div("Generating...").withId("loading-indicator").withClass("htmx-indicator")
                                        ),
                                UiRenderer.resultContainer(article, error)
                        )
                )
        );
    }
}
