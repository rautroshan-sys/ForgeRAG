package com.forgerag.agent.node;

import com.forgerag.agent.AgentState;
import com.forgerag.config.GeminiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web Search Node — CRAG fallback using Gemini Google Search Grounding.
 *
 * Activated by RouterEdge when RetrievalNode.retrievalConfident=false.
 * Calls the Gemini REST API with the googleSearch tool enabled, which
 * instructs Gemini to search the web and ground its answer in live results.
 *
 * Updates AgentState:
 *   - retrievedContext  replaced with the Gemini-grounded response text (as one "chunk")
 *   - webSearchUsed     set to true
 *   - webSearchSources  list of citation URIs from groundingMetadata
 *
 * The answer text from Gemini is used directly as the "retrieved context"
 * so that GenerationNode can synthesise a final response, and EvaluationNode
 * can verify it against the same web-sourced content.
 *
 * Failure handling: if the Gemini search call fails (rate-limit, network, etc.)
 * the node logs a warning and sets retrievedContext to an empty list, allowing
 * the pipeline to continue — EvaluationNode will detect insufficient context.
 */
@Component
public class WebSearchNode {

    private static final Logger log = LoggerFactory.getLogger(WebSearchNode.class);

    // JSON patterns for parsing Gemini REST response
    private static final Pattern TEXT_PATTERN =
            Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern URI_PATTERN =
            Pattern.compile("\"uri\"\\s*:\\s*\"(https?://[^\"]+)\"");
    private static final Pattern SEARCH_URI_PATTERN =
            Pattern.compile("\"webSearchQueries\"\\s*:\\s*\\[(.*?)\\]", Pattern.DOTALL);

    private final GeminiConfig geminiConfig;

    public WebSearchNode(GeminiConfig geminiConfig) {
        this.geminiConfig = geminiConfig;
    }

    public void execute(AgentState state) {
        log.info("WebSearchNode executing for query='{}' (CRAG fallback)", state.getQuery());

        String requestBody = buildRequestBody(state.getOriginalQuery());
        String responseJson = callGeminiSearch(requestBody);

        if (responseJson == null) {
            log.warn("WebSearchNode: Gemini search returned no response — continuing with empty web context");
            state.setRetrievedContext(List.of());
            state.setWebSearchUsed(true);
            return;
        }

        String answerText = extractFirstText(responseJson);
        List<String> sources = extractUris(responseJson);

        log.info("WebSearchNode: got {} chars of web-grounded answer, {} sources", 
                  answerText.length(), sources.size());
        if (!sources.isEmpty()) {
            log.debug("WebSearchNode sources: {}", sources);
        }

        // Replace vector context with the web-grounded answer as a single synthetic chunk
        state.setRetrievedContext(List.of(answerText));
        state.setWebSearchUsed(true);
        state.setWebSearchSources(sources);
    }

    // ── Request builder ──────────────────────────────────────────────────────

    private String buildRequestBody(String query) {
        String escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"");
        return """
                {
                  "contents": [{"parts": [{"text": "%s"}]}],
                  "tools": [{"googleSearch": {}}]
                }
                """.formatted(escapedQuery);
    }

    // ── HTTP call ─────────────────────────────────────────────────────────────

    private String callGeminiSearch(String requestBody) {
        String urlStr = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiConfig.getGeneratorModel()
                + ":generateContent?key=" + geminiConfig.getApiKey();
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            if (status == 200) {
                return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                String error = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                log.warn("WebSearchNode: Gemini API returned HTTP {} — {}", status, error);
                return null;
            }
        } catch (Exception e) {
            log.warn("WebSearchNode: network error calling Gemini Search — {}", e.getMessage());
            return null;
        }
    }

    // ── Response parsers ──────────────────────────────────────────────────────

    /**
     * Extracts the first "text" value from the Gemini JSON response.
     * Gemini nests text inside candidates[0].content.parts[0].text
     */
    private String extractFirstText(String json) {
        Matcher m = TEXT_PATTERN.matcher(json);
        if (m.find()) {
            // Unescape JSON string escapes: \n \t \" \\
            return m.group(1)
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        log.warn("WebSearchNode: could not parse text from Gemini response");
        return "[Web search returned no readable answer]";
    }

    /**
     * Extracts citation URIs from groundingMetadata.groundingChunks[*].web.uri
     */
    private List<String> extractUris(String json) {
        List<String> uris = new ArrayList<>();
        // Only look inside groundingMetadata block to avoid capturing model name URIs
        int metaIdx = json.indexOf("groundingMetadata");
        if (metaIdx < 0) return uris;
        String metaSection = json.substring(metaIdx);
        Matcher m = URI_PATTERN.matcher(metaSection);
        while (m.find()) {
            String uri = m.group(1);
            if (!uris.contains(uri)) uris.add(uri);
        }
        return uris;
    }
}
