package com.teggr.articulate.articles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teggr.articulate.blogs.BlogContent;
import com.teggr.articulate.blogs.BlogGenerationService;
import com.teggr.articulate.transcripts.TranscriptCleaningService;
import com.teggr.articulate.transcripts.TranscriptResult;
import com.teggr.articulate.transcripts.TranscriptService;
import com.teggr.articulate.utils.markdown.MarkdownService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    private static final String URL = "https://www.youtube.com/watch?v=abc1234DEFG";
    private static final TranscriptResult STORED_TRANSCRIPT = new TranscriptResult(
            "trn-123",
            "2026-06-11T09:00:00Z",
            "abc1234DEFG",
            "Source Title",
            "Raw transcript");

    @Mock
    private TranscriptService transcriptService;

    @Mock
    private TranscriptCleaningService transcriptCleaningService;

    @Mock
    private BlogGenerationService blogGenerationService;

    @Mock
    private MarkdownService markdownService;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleIdGenerator articleIdGenerator;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void generatesPersistsMarkdownAndReturnsRenderedHtml() {
        BlogContent generatedBlog = new BlogContent("Generated Title", "# markdown");
        when(transcriptService.fetchAndStore(URL)).thenReturn(STORED_TRANSCRIPT);
        when(articleRepository.findByTranscriptId(STORED_TRANSCRIPT.id())).thenReturn(Optional.empty());
        when(transcriptCleaningService.clean("Raw transcript")).thenReturn("Cleaned transcript");
        when(blogGenerationService.generate("Cleaned transcript")).thenReturn(generatedBlog);
        when(articleIdGenerator.generate()).thenReturn("art-001");
        when(articleRepository.findById("art-001")).thenReturn(Optional.empty());
        when(markdownService.toHtml("# markdown")).thenReturn("<h1>markdown</h1>");

        ArticleResponse response = articleService.generate(new ArticleRequest(URL));

        assertEquals("Generated Title", response.title());
        assertEquals("# markdown", response.markdown());
        assertEquals("<h1>markdown</h1>", response.html());
        verify(articleRepository).save(any(ArticleResult.class));
    }

    @Test
    void reusesExistingArticleForTranscriptAndSkipsRegeneration() {
        ArticleResult existing = new ArticleResult(
                "art-existing",
                "2026-06-11T09:30:00Z",
                STORED_TRANSCRIPT.id(),
                "Existing Title",
                "# existing");

        when(transcriptService.fetchAndStore(URL)).thenReturn(STORED_TRANSCRIPT);
        when(articleRepository.findByTranscriptId(STORED_TRANSCRIPT.id())).thenReturn(Optional.of(existing));
        when(markdownService.toHtml("# existing")).thenReturn("<h1>existing</h1>");

        ArticleResponse response = articleService.generate(new ArticleRequest(URL));

        assertEquals("Existing Title", response.title());
        assertEquals("# existing", response.markdown());
        assertEquals("<h1>existing</h1>", response.html());
        verify(transcriptCleaningService, never()).clean(any());
        verify(blogGenerationService, never()).generate(any());
        verify(articleRepository, never()).save(any());
    }

    @Test
    void retriesWhenGeneratedIdAlreadyExists() {
        BlogContent generatedBlog = new BlogContent("Generated Title", "# markdown");
        ArticleResult existing = new ArticleResult(
                "duplicate",
                "2026-06-11T08:00:00Z",
                "trn-old",
                "Existing",
                "# old");

        when(transcriptService.fetchAndStore(URL)).thenReturn(STORED_TRANSCRIPT);
        when(articleRepository.findByTranscriptId(STORED_TRANSCRIPT.id())).thenReturn(Optional.empty());
        when(transcriptCleaningService.clean("Raw transcript")).thenReturn("Cleaned transcript");
        when(blogGenerationService.generate("Cleaned transcript")).thenReturn(generatedBlog);
        when(articleIdGenerator.generate()).thenReturn("duplicate", "art-002");
        when(articleRepository.findById("duplicate")).thenReturn(Optional.of(existing));
        when(articleRepository.findById("art-002")).thenReturn(Optional.empty());
        when(markdownService.toHtml("# markdown")).thenReturn("<h1>markdown</h1>");

        ArticleResponse response = articleService.generate(new ArticleRequest(URL));

        assertEquals("Generated Title", response.title());
        assertEquals("# markdown", response.markdown());
        assertEquals("<h1>markdown</h1>", response.html());
        verify(articleRepository).save(any(ArticleResult.class));
    }
}
