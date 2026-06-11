package com.teggr.articulate.service.transcripts;

public record TranscriptResult(
        String id,
        String createdAt,
        String videoId,
        String title,
        String transcript
) {

    public static TranscriptResult fetched(String videoId, String title, String transcript) {
        return new TranscriptResult(null, null, videoId, title, transcript);
    }

    public TranscriptResult stored(String id, String createdAt) {
        return new TranscriptResult(id, createdAt, videoId, title, transcript);
    }
}
