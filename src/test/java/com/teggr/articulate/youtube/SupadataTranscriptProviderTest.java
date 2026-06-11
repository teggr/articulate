package com.teggr.articulate.youtube;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.teggr.articulate.service.transcripts.TranscriptResult;
import com.teggr.articulate.youtube.SupadataTranscriptProvider;
import com.teggr.articulate.youtube.YouTubeVideoIdExtractor;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SupadataTranscriptProviderTest {

    @Test
    void encodesNestedYoutubeUrlExactlyOnce() throws Exception {
        AtomicReference<String> rawQuery = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/transcript", new TranscriptHandler(rawQuery));
        server.start();

        try {
            SupadataTranscriptProvider provider = new SupadataTranscriptProvider(
                    new ObjectMapper(),
                    new YouTubeVideoIdExtractor(),
                    "test-api-key",
                    org.springframework.web.client.RestClient.builder()
                            .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                            .build(),
                    1,
                    Duration.ofMillis(100)
            );

            TranscriptResult result = provider.fetchTranscript("https://www.youtube.com/watch?v=nfIcjkR4KZ8");

            String sentQuery = rawQuery.get();
            assertFalse(sentQuery.contains("%25"));
            assertEquals("https://www.youtube.com/watch?v=nfIcjkR4KZ8", extractQueryParam(sentQuery, "url"));
            assertEquals("native", extractQueryParam(sentQuery, "mode"));
            assertEquals("true", extractQueryParam(sentQuery, "text"));
            assertEquals(null, result.id());
            assertEquals(null, result.createdAt());
            assertEquals("nfIcjkR4KZ8", result.videoId());
            assertEquals("Example title", result.title());
            assertEquals("Example transcript", result.transcript());
        } finally {
            server.stop(0);
        }
    }

    private static String extractQueryParam(String rawQuery, String name) {
        String prefix = name + "=";
        for (String pair : rawQuery.split("&")) {
            if (pair.startsWith(prefix)) {
                return java.net.URLDecoder.decode(pair.substring(prefix.length()), StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("Missing query parameter: " + name);
    }

    private static final class TranscriptHandler implements HttpHandler {

        private final AtomicReference<String> rawQuery;

        private TranscriptHandler(AtomicReference<String> rawQuery) {
            this.rawQuery = rawQuery;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            rawQuery.set(exchange.getRequestURI().getRawQuery());
            byte[] body = "{\"title\":\"Example title\",\"content\":\"Example transcript\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(body);
            }
        }
    }
}