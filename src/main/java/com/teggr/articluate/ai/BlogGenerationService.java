package com.teggr.articluate.ai;

import com.teggr.articluate.model.BlogContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

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
        // Step 1: extract structured notes
        log.debug("Generating notes from transcript ({} chars)", cleanedTranscript.length());
        String notesPrompt = new PromptTemplate(notesPromptResource)
                .render(Map.of("transcript", cleanedTranscript));

        String notes = chatClient.prompt()
                .user(notesPrompt)
                .call()
                .content();

        log.debug("Notes generated ({} chars)", notes == null ? 0 : notes.length());

        // Step 2: write blog article from notes
        log.debug("Generating article from notes");
        String articlePrompt = new PromptTemplate(articlePromptResource)
                .render(Map.of("notes", notes));

        String markdown = chatClient.prompt()
                .user(articlePrompt)
                .call()
                .content();

        log.debug("Article generated ({} chars)", markdown == null ? 0 : markdown.length());

        String title = extractTitle(markdown);
        return new BlogContent(title, markdown);
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
