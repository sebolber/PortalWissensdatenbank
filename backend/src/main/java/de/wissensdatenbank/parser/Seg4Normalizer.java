package de.wissensdatenbank.parser;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Normalisiert geparste SEG4-Felder:
 * - Whitespace bereinigen
 * - Schlagworte normalisieren (Trimmen, Deduplizieren)
 * - Leere Felder auf null setzen
 */
@Component
public class Seg4Normalizer {

    /**
     * Normalisiert alle Felder eines ParsedFields-Objekts in-place.
     */
    public Seg4ParsedFields normalize(Seg4ParsedFields fields) {
        fields.setRecommendationNumber(normalizeWhitespace(fields.getRecommendationNumber()));
        fields.setSchlagworte(normalizeKeywords(fields.getSchlagworte()));
        fields.setProblemErlaeuterung(normalizeWhitespace(fields.getProblemErlaeuterung()));
        fields.setEmpfehlung(normalizeWhitespace(fields.getEmpfehlung()));
        fields.setEntscheidung(normalizeWhitespace(fields.getEntscheidung()));
        fields.setZusatzhinweis(normalizeWhitespace(fields.getZusatzhinweis()));
        return fields;
    }

    private String normalizeWhitespace(String value) {
        if (value == null || value.isBlank()) return null;
        return value.replaceAll("\\s+", " ").trim();
    }

    private String normalizeKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) return null;
        return Arrays.stream(keywords.split("[,;|]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.joining(", "));
    }
}
