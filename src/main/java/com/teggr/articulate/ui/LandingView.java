package com.teggr.articulate.ui;

import dev.rebelcraft.j2html.spring.webmvc.J2HtmlView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import j2html.tags.DomContent;
import org.springframework.stereotype.Component;

import java.util.Map;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.main;
import static j2html.TagCreator.meta;
import static j2html.TagCreator.p;
import static j2html.TagCreator.style;
import static j2html.TagCreator.title;

@Component("landingView")
public class LandingView extends J2HtmlView {

    @Override
    protected DomContent renderMergedOutputModelDomContent(Map<String, Object> model,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        String loginUrl = (String) model.getOrDefault("loginUrl", "/login");

        return html(
                head(
                        meta().withCharset("UTF-8"),
                        meta().attr("name", "viewport").attr("content", "width=device-width, initial-scale=1"),
                        title("Articulate"),
                        style("""
                                :root { --bg-0: #0b1220; --bg-1: #17233b; --text: #eef2ff; --muted: #c6d0ea; --card: #ffffff; --card-text: #12203a; --accent: #2ea6ff; }
                                * { box-sizing: border-box; }
                                body { margin: 0; font-family: ui-sans-serif, system-ui, -apple-system, Segoe UI, sans-serif; color: var(--text); background: radial-gradient(circle at 20% 20%, #20335c 0%, var(--bg-0) 40%), linear-gradient(140deg, var(--bg-0), var(--bg-1)); min-height: 100vh; }
                                .page { max-width: 980px; margin: 0 auto; padding: 2rem 1rem 3rem; }
                                .jumbotron { background: linear-gradient(120deg, rgba(46, 166, 255, 0.16), rgba(255, 255, 255, 0.08)); border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 18px; padding: 2.5rem 1.5rem; backdrop-filter: blur(2px); }
                                .jumbotron h1 { margin: 0 0 1rem; font-size: clamp(2rem, 6vw, 3rem); letter-spacing: 0.01em; }
                                .jumbotron p { margin: 0 0 1rem; color: var(--muted); max-width: 70ch; line-height: 1.55; }
                                .panel { margin-top: 1.5rem; background: var(--card); color: var(--card-text); border-radius: 14px; padding: 1.25rem; }
                                .login-cta { display: inline-block; margin-top: 0.75rem; background: var(--accent); color: #04172b; text-decoration: none; font-weight: 700; padding: 0.75rem 1rem; border-radius: 999px; }
                                @media (min-width: 768px) {
                                  .jumbotron { padding: 3.25rem; }
                                  .panel { padding: 1.5rem; }
                                }
                                """)
                ),
                body(
                        main().withClass("page").with(
                                div().withClass("jumbotron").with(
                                        h1("Articulate turns YouTube videos into polished articles."),
                                        p("Paste a video URL and Articulate fetches transcripts, cleans noisy speech patterns, and generates structured long-form writing with readable HTML and Markdown output."),
                                        p("Use it to quickly repurpose talks, tutorials, and interviews into blog-ready content without writing from scratch.")
                                ),
                                div().withClass("panel").with(
                                        p("Ready to generate your first article? Sign in to open the generator workspace."),
                                        a("Log in to continue").withClass("login-cta").withHref(loginUrl)
                                )
                        )
                )
        );
    }
}
