package com.teggr.articulate.youtube;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.teggr.articulate.youtube.CachedTranscriptProvider;
import com.teggr.articulate.youtube.SupadataTranscriptProvider;
import com.teggr.articulate.youtube.TranscriptFileCache;
import com.teggr.articulate.youtube.TranscriptResult;
import com.teggr.articulate.youtube.YouTubeVideoIdExtractor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachedTranscriptProviderTest {

    @Mock
    private TranscriptFileCache cache;

    @Mock
    private SupadataTranscriptProvider delegate;

    @Mock
    private YouTubeVideoIdExtractor videoIdExtractor;

    @InjectMocks
    private CachedTranscriptProvider provider;

    private static final String URL = "https://www.youtube.com/watch?v=abc1234DEFG";
    private static final String VIDEO_ID = "abc1234DEFG";
    private static final TranscriptResult CACHED = new TranscriptResult(VIDEO_ID, "Cached Title", "Cached transcript.");
    private static final TranscriptResult FETCHED = new TranscriptResult(VIDEO_ID, "Fetched Title", "Fetched transcript.");

    @Test
    void returnsCachedResultWithoutCallingDelegate() {
        when(videoIdExtractor.extract(URL)).thenReturn(VIDEO_ID);
        when(cache.load(VIDEO_ID)).thenReturn(Optional.of(CACHED));

        TranscriptResult result = provider.fetchTranscript(URL);

        assertEquals(CACHED, result);
        verifyNoInteractions(delegate);
        verify(cache, never()).save(any());
    }

    @Test
    void fetchesFromDelegateOnCacheMissAndSavesResult() {
        when(videoIdExtractor.extract(URL)).thenReturn(VIDEO_ID);
        when(cache.load(VIDEO_ID)).thenReturn(Optional.empty());
        when(delegate.fetchTranscript(URL)).thenReturn(FETCHED);

        TranscriptResult result = provider.fetchTranscript(URL);

        assertEquals(FETCHED, result);
        verify(delegate).fetchTranscript(URL);
        verify(cache).save(FETCHED);
    }
}
