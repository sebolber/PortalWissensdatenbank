package de.wissensdatenbank.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrahiert strukturierte Felder aus einem SEG4-Rohblock.
 * Keine Businesslogik – nur Regex-basierte Feld-Extraktion.
 */
@Component
public class Seg4FieldExtractor {

    private static final Logger log = LoggerFactory.getLogger(Seg4FieldExtractor.class);

    private static final Pattern PAT_NUMBER = Pattern.compile(
            "(?i)Kodierempfehlung\\s*(?:Nr\\.?\\s*|:\\s*|\\s+)(\\S+)");
    private static final Pattern PAT_SCHLAGWORTE = Pattern.compile(
            "(?i)Schlagwort[e]?\\s*[:/]\\s*(.+?)(?=\\n|$)");
    private static final Pattern PAT_ERSTELLT = Pattern.compile(
            "(?i)erstellt\\s*[:/]?\\s*(\\d{1,2}\\.\\d{1,2}\\.\\d{2,4})");
    private static final Pattern PAT_AKTUALISIERT = Pattern.compile(
            "(?i)aktualisiert\\s*[:/]?\\s*(\\d{1,2}\\.\\d{1,2}\\.\\d{2,4})");
    private static final Pattern PAT_ARBITRATION = Pattern.compile(
            "(?i)SEG\\s*4\\s+hat");

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yy"),
            DateTimeFormatter.ofPattern("d.M.yy")
    );

    /**
     * Extrahiert alle bekannten Felder aus einem Block.
     */
    public Seg4ParsedFields extract(Seg4RawBlock block) {
        String fullText = block.header() + "\n" + block.body();
        Seg4ParsedFields fields = new Seg4ParsedFields();

        fields.setOriginalText(fullText);
        fields.setRecommendationNumber(extractGroup(PAT_NUMBER, fullText));
        fields.setSchlagworte(extractGroup(PAT_SCHLAGWORTE, fullText));
        fields.setErstelltAm(parseDate(extractGroup(PAT_ERSTELLT, fullText)));
        fields.setAktualisiertAm(parseDate(extractGroup(PAT_AKTUALISIERT, fullText)));
        fields.setArbitration(PAT_ARBITRATION.matcher(fullText).find());

        fields.setProblemErlaeuterung(extractSection(fullText, "Problem", "Empfehlung"));
        fields.setEmpfehlung(extractSection(fullText, "Empfehlung", "Entscheidung"));
        fields.setEntscheidung(extractSection(fullText, "Entscheidung", "Zusatzhinweis"));
        fields.setZusatzhinweis(extractSection(fullText, "Zusatzhinweis", null));

        return fields;
    }

    private String extractGroup(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, fmt);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        log.warn("Datumsformat nicht erkannt: {}", dateStr);
        return null;
    }

    /**
     * Extrahiert Text zwischen zwei Sektions-Headern.
     * z.B. zwischen "Problem/Erläuterung:" und "Empfehlung:"
     */
    String extractSection(String text, String startLabel, String endLabel) {
        Pattern start = Pattern.compile(
                "(?i)(?:" + startLabel + ")(?:/Erl[aä]uterung)?\\s*[:/]\\s*", Pattern.UNICODE_CASE);
        Matcher m = start.matcher(text);
        if (!m.find()) return null;

        int begin = m.end();
        int end = text.length();

        if (endLabel != null) {
            Pattern endPat = Pattern.compile(
                    "(?i)(?:" + endLabel + ")(?:/Erl[aä]uterung)?\\s*[:/]", Pattern.UNICODE_CASE);
            Matcher em = endPat.matcher(text);
            if (em.find(begin)) {
                end = em.start();
            }
        }

        String result = text.substring(begin, end).trim();
        return result.isEmpty() ? null : result;
    }
}
