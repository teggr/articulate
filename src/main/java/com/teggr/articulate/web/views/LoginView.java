package com.teggr.articulate.web.views;

import dev.rebelcraft.j2html.spring.webmvc.J2HtmlView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import j2html.tags.DomContent;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;

import java.util.Map;

import static j2html.TagCreator.button;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.main;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

@Component("loginView")
public class LoginView extends J2HtmlView {

    @Override
    protected DomContent renderMergedOutputModelDomContent(Map<String, Object> model,
                                                           HttpServletRequest request,
                                                           HttpServletResponse response) {
        boolean hasError = Boolean.TRUE.equals(model.get("error"));
        boolean loggedOut = Boolean.TRUE.equals(model.get("logout"));
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        return Page.render(
                "Sign in - Articulate",
                "login-page",
                main().withClass("page").with(
                        div().withClass("panel").with(
                                h1("Sign in to Articulate"),
                                hasError ? p("Invalid username or password.").withClass("form-message error") : div(),
                                loggedOut ? p("You have been logged out.").withClass("form-message success") : div(),
                                form().withMethod("post").withAction("/login").with(
                                        csrfToken == null ? div() : input().withType("hidden")
                                                .withName(csrfToken.getParameterName())
                                                .withValue(csrfToken.getToken()),
                                        label("Username").attr("for", "username"),
                                        input().withId("username").withName("username").withType("text").withCondRequired(true),
                                        label("Password").attr("for", "password"),
                                        input().withId("password").withName("password").withType("password").withCondRequired(true),
                                        label().withClass("remember-me").with(
                                                input().withType("checkbox").withName("remember-me"),
                                                span("Remember me")
                                        ),
                                        button("Sign in").withType("submit")
                                )
                        )
                )
        );
    }
}
