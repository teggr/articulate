package com.teggr.articluate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.ai.openai.api-key=test-key",
        "spring.ai.openai.base-url=http://localhost:1234",
        "spring.ai.openai.embedding.base-url=http://localhost:1234"
})
class ArticluateApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context starts successfully
    }
}
