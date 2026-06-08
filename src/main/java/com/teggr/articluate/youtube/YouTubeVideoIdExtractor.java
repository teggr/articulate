package com.teggr.articluate.youtube;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Extracts the 11-char YouTube video ID from supported URL formats or a bare ID.
 */
@Component
public final class YouTubeVideoIdExtractor {

    /**
     * Supports: watch?v=, youtu.be/, shorts/, embed/, v/
     */
    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|shorts/|embed/|v/)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
    );

    private static final Pattern BARE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{11}$");

    public String extract(String input) {
        Matcher matcher = VIDEO_ID_PATTERN.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (BARE_ID_PATTERN.matcher(input).matches()) {
            return input;
        }
        throw new IllegalArgumentException("Cannot extract a YouTube video ID from: " + input);
    }
}
