package de.wissensdatenbank.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Seg4BlockDetectorTest {

    private Seg4BlockDetector detector;

    @BeforeEach
    void setUp() {
        detector = new Seg4BlockDetector();
    }

    @Test
    void detectBlocks_findsMultipleBlocks() {
        String text = """
                Einleitung Text hier.
                Kodierempfehlung Nr. 001
                Problem: Test Problem 1
                Empfehlung: Test Empfehlung 1
                Kodierempfehlung Nr. 002
                Problem: Test Problem 2
                Empfehlung: Test Empfehlung 2
                """;

        List<Seg4RawBlock> blocks = detector.detectBlocks(text);

        assertEquals(2, blocks.size());
        assertTrue(blocks.get(0).header().contains("001"));
        assertTrue(blocks.get(1).header().contains("002"));
    }

    @Test
    void detectBlocks_handlesColonVariant() {
        String text = "Kodierempfehlung: 123\nEmpfehlung: Text hier";

        List<Seg4RawBlock> blocks = detector.detectBlocks(text);

        assertEquals(1, blocks.size());
        assertTrue(blocks.get(0).header().contains("123"));
    }

    @Test
    void detectBlocks_emptyInput() {
        assertEquals(0, detector.detectBlocks("").size());
        assertEquals(0, detector.detectBlocks(null).size());
    }

    @Test
    void detectBlocks_noBlocksFound() {
        String text = "Dies ist ein normaler Text ohne Kodierempfehlungen.";
        assertEquals(0, detector.detectBlocks(text).size());
    }

    @Test
    void detectBlocks_bodyContainsContentBetweenBlocks() {
        String text = """
                Kodierempfehlung Nr. 001
                Inhalt Block 1 Zeile 1
                Inhalt Block 1 Zeile 2
                Kodierempfehlung Nr. 002
                Inhalt Block 2
                """;

        List<Seg4RawBlock> blocks = detector.detectBlocks(text);

        assertTrue(blocks.get(0).body().contains("Inhalt Block 1 Zeile 1"));
        assertTrue(blocks.get(0).body().contains("Inhalt Block 1 Zeile 2"));
        assertFalse(blocks.get(0).body().contains("Inhalt Block 2"));
    }
}
