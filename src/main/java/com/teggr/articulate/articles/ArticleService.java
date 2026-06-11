package com.teggr.articulate.articles;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.teggr.articulate.blogs.BlogContent;
import com.teggr.articulate.blogs.BlogGenerationService;
import com.teggr.articulate.transcripts.TranscriptCleaningService;
import com.teggr.articulate.transcripts.TranscriptResult;
import com.teggr.articulate.transcripts.TranscriptService;
import com.teggr.articulate.utils.markdown.MarkdownService;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private static final int MAX_ID_ATTEMPTS = 5;

    private final TranscriptService transcriptService;
    private final TranscriptCleaningService transcriptCleaningService;
    private final BlogGenerationService blogGenerationService;
    private final MarkdownService markdownService;
    private final ArticleRepository articleRepository;
    private final ArticleIdGenerator articleIdGenerator;

    public ArticleResponse generate(ArticleRequest request) {
        log.info("Generating article for URL: {}", request.youtubeUrl());

        TranscriptResult transcriptResult = transcriptService.fetchAndStore(request.youtubeUrl());
        var existingArticle = articleRepository.findByTranscriptId(transcriptResult.id());
        if (existingArticle.isPresent()) {
            log.info("Reusing cached article {} for transcript {}", existingArticle.get().id(), transcriptResult.id());
            return toResponse(existingArticle.get());
        }

        log.info("Fetched transcript for \"{}\" ({} chars)",
                transcriptResult.title(), transcriptResult.transcript().length());

        String cleaned = transcriptCleaningService.clean(transcriptResult.transcript());
        log.debug("Cleaned transcript: {} chars", cleaned.length());

        BlogContent blog = blogGenerationService.generate(cleaned);
        ArticleResult article = new ArticleResult(
                nextId(),
                Instant.now().toString(),
                transcriptResult.id(),
                blog.title(),
                blog.markdown());
        articleRepository.save(article);
        log.info("Stored article {} for transcript {}", article.id(), article.transcriptId());

        log.info("Article generation complete: \"{}\"", article.title());
        return toResponse(article);
    }

    private ArticleResponse toResponse(ArticleResult article) {
        String html = markdownService.toHtml(article.markdown());
        return new ArticleResponse(article.title(), article.markdown(), html);
    }

    private String nextId() {
        for (int attempt = 0; attempt < MAX_ID_ATTEMPTS; attempt++) {
            String candidate = articleIdGenerator.generate();
            if (articleRepository.findById(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to allocate a unique article id");
    }
}
