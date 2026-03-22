package de.wissensdatenbank.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class Seg4FieldExtractorTest {

    private Seg4FieldExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new Seg4FieldExtractor();
    }

    @Test
    void extract_fullBlock() {
        Seg4RawBlock block = new Seg4RawBlock(
                "Kodierempfehlung Nr. 042",
                """
                Schlagworte: Beatmung, Intensivmedizin, Weaning
                erstellt: 15.03.2023
                aktualisiert: 20.11.2024
                Problem/Erläuterung: Patient wurde 72 Stunden beatmet.
                Empfehlung: Kodierung nach DKR 1001.
                Entscheidung: Hauptdiagnose ist die beatmungspflichtige Erkrankung.
                Zusatzhinweis: Siehe auch Empfehlung Nr. 038.
                """
        );

        Seg4ParsedFields fields = extractor.extract(block);

        assertEquals("042", fields.getRecommendationNumber());
        assertEquals("Beatmung, Intensivmedizin, Weaning", fields.getSchlagworte());
        assertEquals(LocalDate.of(2023, 3, 15), fields.getErstelltAm());
        assertEquals(LocalDate.of(2024, 11, 20), fields.getAktualisiertAm());
        assertNotNull(fields.getProblemErlaeuterung());
        assertTrue(fields.getProblemErlaeuterung().contains("72 Stunden"));
        assertNotNull(fields.getEmpfehlung());
        assertTrue(fields.getEmpfehlung().contains("DKR 1001"));
        assertNotNull(fields.getEntscheidung());
        assertNotNull(fields.getZusatzhinweis());
        assertFalse(fields.isArbitration());
    }

    @Test
    void extract_detectsArbitration() {
        Seg4RawBlock block = new Seg4RawBlock(
                "Kodierempfehlung Nr. 099",
                "SEG 4 hat entschieden, dass die Kodierung korrekt ist."
        );

        Seg4ParsedFields fields = extractor.extract(block);

        assertTrue(fields.isArbitration());
    }

    @Test
    void extract_preservesOriginalText() {
        Seg4RawBlock block = new Seg4RawBlock("Kodierempfehlung Nr. 001", "Body text");
        Seg4ParsedFields fields = extractor.extract(block);
        assertTrue(fields.getOriginalText().contains("Kodierempfehlung Nr. 001"));
        assertTrue(fields.getOriginalText().contains("Body text"));
    }

    @Test
    void extract_handlesMinimalBlock() {
        Seg4RawBlock block = new Seg4RawBlock("Kodierempfehlung Nr. 001", "");
        Seg4ParsedFields fields = extractor.extract(block);
        assertEquals("001", fields.getRecommendationNumber());
        assertNull(fields.getSchlagworte());
        assertNull(fields.getErstelltAm());
    }
}
