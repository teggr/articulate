package com.teggr.articulate.transcripts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.teggr.articulate.youtube.YouTubeVideoIdExtractor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptServiceTest {

    private static final String URL = "https://www.youtube.com/watch?v=abc1234DEFG";
    private static final TranscriptResult FETCHED = TranscriptResult.fetched(
            "abc1234DEFG",
            "Fetched Title",
            "Fetched transcript.");

    @Mock
    private TranscriptProvider transcriptProvider;

    @Mock
    private TranscriptRepository transcriptRepository;

    @Mock
    private TranscriptIdGenerator transcriptIdGenerator;

    @Mock
    private YouTubeVideoIdExtractor videoIdExtractor;

    @InjectMocks
    private TranscriptService transcriptService;

    @Test
    void fetchesTranscriptAssignsMetadataAndSavesIt() {
        when(videoIdExtractor.extract(URL)).thenReturn(FETCHED.videoId());
        when(transcriptRepository.findByVideoId(FETCHED.videoId())).thenReturn(Optional.empty());
        when(transcriptProvider.fetchTranscript(URL)).thenReturn(FETCHED);
        when(transcriptIdGenerator.generate()).thenReturn("short123");
        when(transcriptRepository.findById("short123")).thenReturn(Optional.empty());

        TranscriptResult result = transcriptService.fetchAndStore(URL);

        assertEquals("short123", result.id());
        assertNotNull(result.createdAt());
        assertEquals(Instant.parse(result.createdAt()).toString(), result.createdAt());
        assertEquals(FETCHED.videoId(), result.videoId());
        assertEquals(FETCHED.title(), result.title());
        assertEquals(FETCHED.transcript(), result.transcript());
        verify(transcriptRepository).save(result);
    }

    @Test
    void reusesExistingTranscriptForVideoIdWithoutRefetching() {
        TranscriptResult existing = new TranscriptResult(
                "existing123",
                "2026-06-10T22:00:00Z",
                FETCHED.videoId(),
                "Existing",
                "Existing transcript");

        when(videoIdExtractor.extract(URL)).thenReturn(FETCHED.videoId());
        when(transcriptRepository.findByVideoId(FETCHED.videoId())).thenReturn(Optional.of(existing));

        TranscriptResult result = transcriptService.fetchAndStore(URL);

        assertEquals(existing, result);
        verify(transcriptProvider, never()).fetchTranscript(URL);
        verify(transcriptRepository, never()).save(any());
    }

    @Test
    void retriesWhenGeneratedIdAlreadyExists() {
        TranscriptResult existing = new TranscriptResult(
                "duplicate",
                "2026-06-10T22:00:00Z",
                "old-video",
                "Existing",
                "Existing transcript");

        when(videoIdExtractor.extract(URL)).thenReturn(FETCHED.videoId());
        when(transcriptRepository.findByVideoId(FETCHED.videoId())).thenReturn(Optional.empty());
        when(transcriptProvider.fetchTranscript(URL)).thenReturn(FETCHED);
        when(transcriptIdGenerator.generate()).thenReturn("duplicate", "short123");
        when(transcriptRepository.findById("duplicate")).thenReturn(Optional.of(existing));
        when(transcriptRepository.findById("short123")).thenReturn(Optional.empty());

        TranscriptResult result = transcriptService.fetchAndStore(URL);

        assertEquals("short123", result.id());
        verify(transcriptRepository).save(result);
    }
}
