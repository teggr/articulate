package com.teggr.articluate.youtube;

public record TranscriptResult(
        String videoId,
        String title,
        String transcript
) {
}
