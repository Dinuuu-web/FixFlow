package com.fixflow.ai;

import com.fixflow.dto.AiRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Gemini AI Service - calls Google Generative Language API.
 * API key is read from GEMINI_API_KEY environment variable.
 * If key is absent, returns a graceful fallback message.
 */
@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent}")
    private String apiUrl;

    @Value("${gemini.connect.timeout:10000}")
    private int connectTimeout;

    @Value("${gemini.read.timeout:30000}")
    private int readTimeout;

    @Value("${gemini.max.input.length:4000}")
    private int maxInputLength;

    private static final String UNAVAILABLE_MSG =
        "AI assistant is currently unavailable. Please configure the GEMINI_API_KEY environment variable.";

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Improve and professionalize a complaint draft.
     */
    public String improveComplaint(String draft) {
        if (!isConfigured()) return UNAVAILABLE_MSG;
        String truncated = truncate(draft);
        String prompt = """
            You are a professional campus complaint assistant. Rewrite the following complaint \
            to be clear, professional, and detailed. Preserve the original intent. \
            Do NOT invent new facts or events. Return only the rewritten complaint text, \
            no additional commentary.

            Original complaint:
            """ + truncated;
        return callGemini(prompt);
    }

    /**
     * Suggest a complaint category.
     */
    public String suggestCategory(String description) {
        if (!isConfigured()) return UNAVAILABLE_MSG;
        String prompt = """
            Based on the following campus complaint description, suggest the most appropriate \
            category from this list:
            - Furniture & Fixtures
            - Electrical Issues
            - Plumbing & Water
            - IT & Computer Equipment
            - Projector & AV Equipment
            - Wi-Fi & Networking
            - Cleanliness & Sanitation
            - Structural & Civil
            - Safety & Security
            - Other

            Return the category name and a 1-2 sentence explanation.

            Description:
            """ + truncate(description);
        return callGemini(prompt);
    }

    /**
     * Suggest a priority level.
     */
    public String suggestPriority(String description) {
        if (!isConfigured()) return UNAVAILABLE_MSG;
        String prompt = """
            Based on the following campus complaint description, suggest a priority level \
            from: LOW, MEDIUM, HIGH, or CRITICAL. \
            Use CRITICAL for immediate safety hazards (electrical, flooding, fire risk). \
            Use HIGH for issues affecting multiple users or essential services. \
            Use MEDIUM for moderate inconveniences. \
            Use LOW for cosmetic or minor issues. \
            Return the priority and a brief explanation of why.

            Description:
            """ + truncate(description);
        return callGemini(prompt);
    }

    /**
     * Provide basic troubleshooting guidance.
     */
    public String troubleshootingGuidance(String description) {
        if (!isConfigured()) return UNAVAILABLE_MSG;
        String prompt = """
            You are a campus facilities assistant. Provide safe, basic troubleshooting steps \
            for the following campus issue. \
            IMPORTANT SAFETY RULES: \
            - For any electrical hazard, water near electrical equipment, or dangerous situation, \
              tell the user to stay away and contact authorized maintenance staff immediately. \
            - Do not recommend any dangerous repairs or actions. \
            - Keep suggestions safe for non-technical students. \
            - Return numbered steps.

            Issue:
            """ + truncate(description);
        return callGemini(prompt);
    }

    /**
     * Generate an admin summary of a complaint.
     */
    public String adminSummary(String title, String description, String category,
                               String priority, String location, String status) {
        if (!isConfigured()) return UNAVAILABLE_MSG;
        String input = String.format(
            "Title: %s\nCategory: %s\nPriority: %s\nLocation: %s\nStatus: %s\nDescription: %s",
            title, category, priority, location, status, truncate(description)
        );
        String prompt = """
            Summarize the following campus complaint for an administrator. \
            Be concise. Suggest the appropriate next administrative action. \
            Return: 1) A 2-3 sentence summary. 2) Recommended next action.

            Complaint:
            """ + input;
        return callGemini(prompt);
    }

    // ============================================
    // INTERNAL: Call Gemini REST API
    // ============================================
    private String callGemini(String prompt) {
        try {
            String requestBody = buildRequestBody(prompt);
            String url = apiUrl + "?key=" + apiKey;

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(readTimeout))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractText(response.body());
            } else {
                log.warn("Gemini API returned status {}: {}", response.statusCode(), response.body());
                return "AI service returned an error. Please try again later.";
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Gemini API call interrupted", e);
            return "AI request was interrupted. Please try again.";
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            return "AI service is temporarily unavailable. Please try again later.";
        }
    }

    private String buildRequestBody(String prompt) {
        // Escape JSON special characters
        String escaped = prompt
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");

        return """
            {
              "contents": [
                {
                  "parts": [
                    {"text": "%s"}
                  ]
                }
              ],
              "generationConfig": {
                "temperature": 0.4,
                "maxOutputTokens": 1024
              }
            }
            """.formatted(escaped);
    }

    private String extractText(String jsonBody) {
        // Simple JSON parsing to avoid extra dependencies
        // Looks for "text": "..." pattern
        try {
            String marker = "\"text\":";
            int idx = jsonBody.indexOf(marker);
            if (idx == -1) return "No response generated.";

            int start = jsonBody.indexOf('"', idx + marker.length()) + 1;
            int end = start;
            boolean escape = false;
            StringBuilder sb = new StringBuilder();
            while (end < jsonBody.length()) {
                char c = jsonBody.charAt(end);
                if (escape) {
                    switch (c) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        default -> sb.append(c);
                    }
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
                end++;
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("Failed to parse Gemini response", e);
            return "Could not parse AI response.";
        }
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > maxInputLength ? text.substring(0, maxInputLength) + "..." : text;
    }
}
