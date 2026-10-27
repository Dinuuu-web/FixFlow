package com.fixflow.ai;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class GeminiServiceTest {

    @Test
    void testNotConfiguredReturnsGracefulMessage() {
        GeminiService service = new GeminiService();
        ReflectionTestUtils.setField(service, "apiKey", "");
        assertFalse(service.isConfigured());

        String result = service.improveComplaint("The light is broken");
        assertTrue(result.contains("AI assistant is currently unavailable"));
    }

    @Test
    void testTruncate() {
        GeminiService service = new GeminiService();
        ReflectionTestUtils.setField(service, "maxInputLength", 10);
        String truncated = (String) ReflectionTestUtils.invokeMethod(service, "truncate", "1234567890123");
        assertEquals("1234567890...", truncated);
    }

    @Test
    void testExtractTextFromResponse() {
        GeminiService service = new GeminiService();
        String sampleJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "Rewritten complaint text successfully."
                      }
                    ]
                  }
                }
              ]
            }
            """;
        String extracted = (String) ReflectionTestUtils.invokeMethod(service, "extractText", sampleJson);
        assertEquals("Rewritten complaint text successfully.", extracted);
    }
}
