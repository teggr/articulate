package com.teggr.articulate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.teggr.articulate.ai.BlogGenerationService;
import com.teggr.articulate.model.ArticleRequest;
import com.teggr.articulate.model.ArticleResponse;
import com.teggr.articulate.model.BlogContent;
import com.teggr.articulate.service.transcripts.TranscriptCleaningService;
import com.teggr.articulate.service.transcripts.TranscriptResult;
import com.teggr.articulate.service.transcripts.TranscriptService;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final TranscriptService transcriptService;
    private final TranscriptCleaningService transcriptCleaningService;
    private final BlogGenerationService blogGenerationService;
    private final MarkdownService markdownService;

    public ArticleResponse generate(ArticleRequest request) {
        log.info("Generating article for URL: {}", request.youtubeUrl());

        TranscriptResult transcriptResult = transcriptService.fetchAndStore(request.youtubeUrl());
        log.info("Fetched transcript for \"{}\" ({} chars)",
                transcriptResult.title(), transcriptResult.transcript().length());

        String cleaned = transcriptCleaningService.clean(transcriptResult.transcript());
        log.debug("Cleaned transcript: {} chars", cleaned.length());

        BlogContent blog = blogGenerationService.generate(cleaned);

        String html = markdownService.toHtml(blog.markdown());

        log.info("Article generation complete: \"{}\"", blog.title());
        return new ArticleResponse(blog.title(), blog.markdown(), html);
    }
}
