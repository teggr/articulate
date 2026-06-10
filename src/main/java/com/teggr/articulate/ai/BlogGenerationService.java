package com.teggr.articulate.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.teggr.articulate.model.BlogContent;

import java.util.Arrays;
import java.util.Map;
import java.lang.reflect.Method;

/**
 * Two-step AI pipeline:
 * <ol>
 *   <li><strong>Notes</strong> — extract key insights from the cleaned transcript.</li>
 *   <li><strong>Article</strong> — write a blog post from those notes.</li>
 * </ol>
 *
 * <p>Splitting the work into two prompts keeps each prompt focused and yields
 * noticeably better output quality than a single "transcript → article" prompt.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogGenerationService {

    private final ChatClient chatClient;

        @Value("${articulate.ai.max-transcript-chars:0}")
        private int maxTranscriptChars;

    @Value("classpath:prompts/notes.st")
    private Resource notesPromptResource;

    @Value("classpath:prompts/article.st")
    private Resource articlePromptResource;

    /**
     * Generates a {@link BlogContent} (title + markdown body) from the given
     * cleaned transcript text.
     *
     * @param cleanedTranscript whitespace-normalised, noise-free transcript
     * @return blog content ready for rendering
     */
    public BlogContent generate(String cleanedTranscript) {
        String effectiveTranscript = applyTranscriptCap(cleanedTranscript);

        // Step 1: extract structured notes
        log.info("AI step=notes transcriptChars={} cap={}",
                effectiveTranscript.length(),
                maxTranscriptChars > 0 ? maxTranscriptChars : "disabled");
        String notesPrompt = new PromptTemplate(notesPromptResource)
                .render(Map.of("transcript", effectiveTranscript));

        log.info("AI step=notes promptChars={}", notesPrompt.length());

        String notes;
        try {
            notes = chatClient.prompt()
                    .user(notesPrompt)
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            logAiFailure("notes", notesPrompt.length(), ex);
            throw ex;
        }

        log.debug("Notes generated ({} chars)", notes == null ? 0 : notes.length());

        // Step 2: write blog article from notes
        log.debug("Generating article from notes");
        String articlePrompt = new PromptTemplate(articlePromptResource)
                .render(Map.of("notes", notes));

        log.info("AI step=article promptChars={} notesChars={}",
                articlePrompt.length(),
                notes == null ? 0 : notes.length());

        String markdown;
        try {
            markdown = chatClient.prompt()
                    .user(articlePrompt)
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            logAiFailure("article", articlePrompt.length(), ex);
            throw ex;
        }

        log.debug("Article generated ({} chars)", markdown == null ? 0 : markdown.length());

        String title = extractTitle(markdown);
        return new BlogContent(title, markdown);
    }

    private String applyTranscriptCap(String cleanedTranscript) {
        if (maxTranscriptChars <= 0 || cleanedTranscript.length() <= maxTranscriptChars) {
            return cleanedTranscript;
        }

        log.warn("Capping transcript from {} to {} chars for AI call diagnostics",
                cleanedTranscript.length(), maxTranscriptChars);
        return cleanedTranscript.substring(0, maxTranscriptChars);
    }

    private void logAiFailure(String step, int promptChars, RuntimeException ex) {
        String statusCode = tryInvoke(ex, "statusCode");
        String requestId = tryInvoke(ex, "requestId");
        String body = tryInvoke(ex, "body");
        String code = tryInvoke(ex, "code");
        String type = tryInvoke(ex, "type");

        log.error("AI failure step={} promptChars={} exceptionType={} message={} statusCode={} requestId={} code={} type={} body={}",
                step,
                promptChars,
                ex.getClass().getName(),
                ex.getMessage(),
                statusCode,
                requestId,
                code,
                type,
                body,
                ex);
    }

    private String tryInvoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value == null ? "null" : value.toString();
        } catch (Exception ignored) {
            return "n/a";
        }
    }

    /**
     * Extracts the first H1 heading from the Markdown as the article title.
     * Falls back to "Generated Article" if none is found.
     */
    private String extractTitle(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "Generated Article";
        }
        return Arrays.stream(markdown.split("\n"))
                .filter(line -> line.startsWith("# "))
                .findFirst()
                .map(line -> line.substring(2).strip())
                .orElse("Generated Article");
    }
}
