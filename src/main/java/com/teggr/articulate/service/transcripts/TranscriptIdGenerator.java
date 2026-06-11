package com.teggr.articulate.service.transcripts;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TranscriptIdGenerator {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[6];
        secureRandom.nextBytes(bytes);
        return ENCODER.encodeToString(bytes);
    }
}
