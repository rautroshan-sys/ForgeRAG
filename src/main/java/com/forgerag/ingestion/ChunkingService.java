package com.forgerag.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Structure-aware document chunker.
 *
 * Strategy: split on Markdown/plain-text heading boundaries (## / ###) and
 * on double-newlines (blank lines between paragraphs). Each chunk carries
 * its nearest preceding heading as context so retrieved context is never
 * semantically decapitated mid-section.
 *
 * Chunks are then capped at MAX_CHARS and hard-split if one section is huge.
 * Empty or whitespace-only segments are discarded.
 *
 * This is structure-aware rather than fixed-size (no sliding window),
 * which preserves paragraph and section hierarchy. See ARCHITECTURE.md.
 */
@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    // Split on lines that look like Markdown headings (# / ## / ###)
    private static final Pattern HEADING_PATTERN =
            Pattern.compile("(?m)^(#{1,3}\\s.+)$");

    // Max characters per chunk before hard-splitting
    private static final int MAX_CHARS = 1500;
    // Min characters — discard micro-fragments
    private static final int MIN_CHARS = 40;

    /**
     * Splits document text into structure-aware chunks.
     *
     * @param text raw document text (Markdown or plain text)
     * @return list of non-empty chunks, each preserving its section header
     */
    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        String[] lines = text.split("\n");

        StringBuilder currentSection = new StringBuilder();
        String currentHeading = "";

        for (String line : lines) {
            if (HEADING_PATTERN.matcher(line.trim()).matches()) {
                // Flush the current section before starting a new one
                flushSection(currentHeading, currentSection.toString(), result);
                currentHeading = line.trim();
                currentSection = new StringBuilder();
            } else {
                currentSection.append(line).append("\n");
            }
        }
        // Flush the last section
        flushSection(currentHeading, currentSection.toString(), result);

        log.debug("ChunkingService produced {} chunks from {} characters", result.size(), text.length());
        return result;
    }

    private void flushSection(String heading, String body, List<String> result) {
        String[] paragraphs = body.split("\n\n+");
        StringBuilder buffer = new StringBuilder();
        if (!heading.isBlank()) buffer.append(heading).append("\n\n");

        for (String para : paragraphs) {
            String p = para.strip();
            if (p.isEmpty()) continue;

            if (buffer.length() + p.length() > MAX_CHARS && buffer.length() > 0) {
                addIfValid(buffer.toString(), result);
                // New buffer starts with the heading so context is preserved
                buffer = new StringBuilder();
                if (!heading.isBlank()) buffer.append(heading).append(" (cont.)\n\n");
            }
            buffer.append(p).append("\n\n");
        }
        if (buffer.length() > 0) {
            addIfValid(buffer.toString(), result);
        }
    }

    private void addIfValid(String chunk, List<String> result) {
        String trimmed = chunk.strip();
        if (trimmed.length() >= MIN_CHARS) {
            result.add(trimmed);
        }
    }
}
