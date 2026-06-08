package com.teggr.articluate.service;

import com.teggr.articluate.ai.BlogGenerationService;
import com.teggr.articluate.model.ArticleRequest;
import com.teggr.articluate.model.ArticleResponse;
import com.teggr.articluate.model.BlogContent;
import com.teggr.articluate.youtube.TranscriptProvider;
import com.teggr.articluate.youtube.TranscriptResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final TranscriptProvider transcriptProvider;
    private final TranscriptCleaningService transcriptCleaningService;
    private final BlogGenerationService blogGenerationService;
    private final MarkdownService markdownService;

    public ArticleResponse generate(ArticleRequest request) {
        log.info("Generating article for URL: {}", request.youtubeUrl());

        TranscriptResult transcriptResult = transcriptProvider.fetchTranscript(request.youtubeUrl());
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
