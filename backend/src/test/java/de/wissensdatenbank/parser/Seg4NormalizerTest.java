package de.wissensdatenbank.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Seg4NormalizerTest {

    private Seg4Normalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new Seg4Normalizer();
    }

    @Test
    void normalize_removesExcessWhitespace() {
        Seg4ParsedFields fields = new Seg4ParsedFields();
        fields.setProblemErlaeuterung("  Viel   Whitespace   hier  ");

        Seg4ParsedFields result = normalizer.normalize(fields);

        assertEquals("Viel Whitespace hier", result.getProblemErlaeuterung());
    }

    @Test
    void normalize_deduplicatesKeywords() {
        Seg4ParsedFields fields = new Seg4ParsedFields();
        fields.setSchlagworte("Beatmung, Intensiv, Beatmung, Weaning");

        Seg4ParsedFields result = normalizer.normalize(fields);

        assertEquals("Beatmung, Intensiv, Weaning", result.getSchlagworte());
    }

    @Test
    void normalize_handlesNullFields() {
        Seg4ParsedFields fields = new Seg4ParsedFields();
        Seg4ParsedFields result = normalizer.normalize(fields);
        assertNull(result.getProblemErlaeuterung());
        assertNull(result.getSchlagworte());
    }

    @Test
    void normalize_blankToNull() {
        Seg4ParsedFields fields = new Seg4ParsedFields();
        fields.setEmpfehlung("   ");

        Seg4ParsedFields result = normalizer.normalize(fields);

        assertNull(result.getEmpfehlung());
    }
}
