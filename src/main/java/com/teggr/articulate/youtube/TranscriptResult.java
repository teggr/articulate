package com.teggr.articulate.youtube;

public record TranscriptResult(
        String videoId,
        String title,
        String transcript
) {
}
