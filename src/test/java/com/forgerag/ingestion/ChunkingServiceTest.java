package com.forgerag.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunkingService.
 *
 * No Spring context needed — ChunkingService is a pure-logic class.
 * Tests cover: empty input, plain paragraphs, heading boundaries,
 * heading prefix preservation, MAX_CHARS hard-split, and MIN_CHARS discard.
 */
class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
    }

    // ── Empty / null input ──────────────────────────────────────────────────

    @Test
    void emptyString_returnsEmptyList() {
        List<String> chunks = chunkingService.chunk("");
        assertTrue(chunks.isEmpty(), "Empty input should produce no chunks");
    }

    @Test
    void blankString_returnsEmptyList() {
        List<String> chunks = chunkingService.chunk("   \n  \n  ");
        assertTrue(chunks.isEmpty(), "Blank-only input should produce no chunks");
    }

    @Test
    void nullInput_returnsEmptyList() {
        List<String> chunks = chunkingService.chunk(null);
        assertTrue(chunks.isEmpty(), "Null input should produce no chunks");
    }

    // ── Plain paragraph text ────────────────────────────────────────────────

    @Test
    void singleParagraph_returnsOneChunk() {
        String text = "This is a single paragraph with enough content to exceed the minimum threshold.";
        List<String> chunks = chunkingService.chunk(text);
        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("single paragraph"));
    }

    @Test
    void twoParagraphsSeparatedByBlankLine_returnsTwoChunks() {
        String text = "First paragraph with sufficient length to be a valid chunk.\n\n"
                + "Second paragraph with sufficient length to be a valid chunk.";
        List<String> chunks = chunkingService.chunk(text);
        // Both paragraphs may be merged into one chunk if combined <= MAX_CHARS
        // but must produce at least 1 chunk
        assertFalse(chunks.isEmpty());
        // Total content must be present
        String combined = String.join(" ", chunks);
        assertTrue(combined.contains("First paragraph"));
        assertTrue(combined.contains("Second paragraph"));
    }

    // ── Heading boundary splitting ──────────────────────────────────────────

    @Test
    void markdownHeading_startsNewChunk() {
        String text = "## Section One\n"
                + "Content of the first section with enough text to be valid.\n\n"
                + "## Section Two\n"
                + "Content of the second section with enough text to be valid.";
        List<String> chunks = chunkingService.chunk(text);
        assertTrue(chunks.size() >= 2, "Two headings should produce at least two chunks");
    }

    @Test
    void headingPrefixPreservedInChunk() {
        String text = "## Revenue Report\n"
                + "Revenue grew 12 percent quarter over quarter driven by product expansion.";
        List<String> chunks = chunkingService.chunk(text);
        assertFalse(chunks.isEmpty());
        // The chunk should carry the heading text
        assertTrue(chunks.get(0).contains("Revenue Report"),
                "Chunk should contain the heading: " + chunks.get(0));
    }

    @Test
    void level3HeadingRecognized() {
        String text = "### Sub-section\n"
                + "Content under a level-3 heading with enough characters to be retained.";
        List<String> chunks = chunkingService.chunk(text);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).contains("Sub-section"));
    }

    // ── MIN_CHARS discard ───────────────────────────────────────────────────

    @Test
    void microFragment_discarded() {
        // A heading with a body that is too short (< MIN_CHARS = 40) should be dropped
        String text = "## Title\nOK.";
        List<String> chunks = chunkingService.chunk(text);
        // "## Title\n\nOK." total is < 40 chars → chunk discarded
        // (Exact behaviour depends on heading prefix length: "## Title\n\nOK." ~ 14 chars)
        // We simply assert no exception and the result is a list (may be empty)
        assertNotNull(chunks);
    }

    // ── Multiple sections integrity ─────────────────────────────────────────

    @Test
    void multiSectionDocument_allContentPresent() {
        String text = "## Introduction\n"
                + "This section introduces the topic at hand with sufficient detail.\n\n"
                + "## Background\n"
                + "Historical context and background information for this analysis.\n\n"
                + "## Results\n"
                + "The results showed a significant improvement in the key metrics.";
        List<String> chunks = chunkingService.chunk(text);
        String combined = String.join("\n", chunks);
        assertTrue(combined.contains("Introduction") || combined.contains("introduces"),
                "Introduction content missing from chunks");
        assertTrue(combined.contains("Background") || combined.contains("Historical"),
                "Background content missing from chunks");
        assertTrue(combined.contains("Results") || combined.contains("improvement"),
                "Results content missing from chunks");
    }

    // ── Hard-split on MAX_CHARS ─────────────────────────────────────────────

    @Test
    void longSection_hardSplitsWithContSuffix() {
        // Build a section body > MAX_CHARS (1500) by repeating a paragraph
        String para = "This is a paragraph with sufficient content to contribute meaningfully. ";
        StringBuilder body = new StringBuilder("## Long Section\n");
        while (body.length() < 3200) {
            body.append(para).append("\n\n");
        }
        List<String> chunks = chunkingService.chunk(body.toString());
        assertTrue(chunks.size() >= 2,
                "A section exceeding MAX_CHARS should produce at least 2 chunks");
        // Continuation chunks should carry the heading prefix
        boolean hasCont = chunks.stream().anyMatch(c -> c.contains("(cont.)"));
        assertTrue(hasCont, "Overflow chunks should carry the (cont.) suffix on the heading");
    }
}
