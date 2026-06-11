package com.teggr.articulate.service.transcripts;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TranscriptService {

    private static final int MAX_ID_ATTEMPTS = 5;

    private final TranscriptProvider transcriptProvider;
    private final TranscriptRepository transcriptRepository;
    private final TranscriptIdGenerator transcriptIdGenerator;

    public TranscriptResult fetchAndStore(String youtubeUrl) {
        TranscriptResult fetchedTranscript = transcriptProvider.fetchTranscript(youtubeUrl);
        TranscriptResult storedTranscript = fetchedTranscript.stored(nextId(), Instant.now().toString());
        transcriptRepository.save(storedTranscript);
        log.info("Stored transcript {} for video {}", storedTranscript.id(), storedTranscript.videoId());
        return storedTranscript;
    }

    private String nextId() {
        for (int attempt = 0; attempt < MAX_ID_ATTEMPTS; attempt++) {
            String candidate = transcriptIdGenerator.generate();
            if (transcriptRepository.findById(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Failed to allocate a unique transcript id");
    }
}
