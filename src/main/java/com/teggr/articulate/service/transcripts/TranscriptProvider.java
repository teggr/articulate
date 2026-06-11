package com.teggr.articulate.service.transcripts;

public interface TranscriptProvider {

    /**
     * Fetches the transcript for the given YouTube URL.
     *
     * @param youtubeUrl the full YouTube video URL (or bare video ID)
     * @return a {@link TranscriptResult} containing the video metadata and transcript text
     * @throws com.teggr.articulate.exception.TranscriptNotFoundException if no transcript is available
     * @throws IllegalArgumentException                                    if the URL cannot be parsed
     */
    TranscriptResult fetchTranscript(String youtubeUrl);
}
