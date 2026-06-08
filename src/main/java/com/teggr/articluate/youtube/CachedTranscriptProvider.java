package com.teggr.articluate.youtube;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * {@link TranscriptProvider} decorator that checks a local file cache before
 * delegating to {@link YoutubeTranscriptProvider}.
 *
 * <p>Cache layout: {@code .data/{videoId}.json} (directory configurable via
 * {@code articluate.transcript-cache.dir}).</p>
 */
@Primary
@Component
@RequiredArgsConstructor
@Slf4j
public class CachedTranscriptProvider implements TranscriptProvider {

    private final TranscriptFileCache cache;
    private final YoutubeTranscriptProvider delegate;
    private final YouTubeVideoIdExtractor videoIdExtractor;

    @Override
    public TranscriptResult fetchTranscript(String youtubeUrl) {
        String videoId = videoIdExtractor.extract(youtubeUrl);

        return cache.load(videoId).orElseGet(() -> {
            log.debug("Cache miss for video ID: {} — fetching from YouTube", videoId);
            TranscriptResult result = delegate.fetchTranscript(youtubeUrl);
            cache.save(result);
            return result;
        });
    }
}
