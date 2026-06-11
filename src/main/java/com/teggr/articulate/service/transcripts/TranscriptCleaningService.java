package com.teggr.articulate.service.transcripts;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Strips noise from raw YouTube transcripts to reduce token usage and improve
 * AI output quality.
 *
 * <p>Removes:</p>
 * <ul>
 *   <li>Sound/action tags: {@code [Music]}, {@code [Applause]}, etc.</li>
 *   <li>Filler words: <em>uh</em>, <em>uhh</em>, <em>um</em>, <em>umm</em></li>
 *   <li>Excess whitespace</li>
 * </ul>
 */
@Service
public class TranscriptCleaningService {

    /** Matches anything inside square brackets, e.g. [Music], [Applause]. */
    private static final Pattern BRACKET_TAG_PATTERN =
            Pattern.compile("\\[[^\\]]*\\]", Pattern.CASE_INSENSITIVE);

    /**
     * Matches isolated filler sounds ({@code uh}, {@code uhh}, {@code um}, {@code umm})
     * as whole words, optionally followed by a comma.
     */
    private static final Pattern FILLER_SOUND_PATTERN =
            Pattern.compile("\\buh+\\b,?|\\bum+\\b,?", Pattern.CASE_INSENSITIVE);

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s{2,}");

    public String clean(String transcript) {
        String result = transcript;
        result = BRACKET_TAG_PATTERN.matcher(result).replaceAll(" ");
        result = FILLER_SOUND_PATTERN.matcher(result).replaceAll(" ");
        result = MULTI_SPACE_PATTERN.matcher(result).replaceAll(" ");
        return result.strip();
    }
}
