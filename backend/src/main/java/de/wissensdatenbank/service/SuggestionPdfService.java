package de.wissensdatenbank.service;

import de.wissensdatenbank.dto.SuggestionResponse;
import de.wissensdatenbank.entity.DocumentSuggestion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Erzeugt PDF-Dokumente aus Kodierempfehlungen.
 */
@Service
public class SuggestionPdfService {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 14;
    private static final float HEADING_HEIGHT = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Erzeugt ein PDF mit den Kodierempfehlungen.
     */
    public byte[] generateResultPdf(DocumentSuggestion ds, List<String> empfehlungen,
                                     List<SuggestionResponse.UsedSource> quellen) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float pageWidth = PDRectangle.A4.getWidth();
            float usableWidth = pageWidth - 2 * MARGIN;

            ContentWriter writer = new ContentWriter(doc, fontRegular, fontBold, usableWidth);

            // Titel
            writer.writeHeading("KI-Kodierempfehlung: " + ds.getFileName());
            writer.writeLine("");
            writer.writeLine("Erstellt: " + (ds.getCreatedAt() != null ? ds.getCreatedAt().format(DATE_FMT) : "-"));
            if (ds.getLlmModel() != null) {
                writer.writeLine("Modell: " + ds.getLlmModel() + "  |  Tokens: " + ds.getTokenCount());
            }
            writer.writeLine("");

            // Empfehlungen
            for (int i = 0; i < empfehlungen.size(); i++) {
                writer.writeHeading("Empfehlung " + (i + 1));
                for (String line : empfehlungen.get(i).split("\n")) {
                    writer.writeLine(line);
                }
                writer.writeLine("");
            }

            // Quellen
            if (quellen != null && !quellen.isEmpty()) {
                writer.writeHeading("Verwendete Quellen");
                for (SuggestionResponse.UsedSource q : quellen) {
                    writer.writeLine("- " + q.title() + " [" + q.bindingLevel() + "] " + q.matchReason());
                }
            }

            writer.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Erzeugt ein annotiertes Dokument-PDF mit gelb markierten relevanten Passagen.
     */
    public byte[] generateAnnotatedPdf(DocumentSuggestion ds, List<String> empfehlungen,
                                        List<SuggestionResponse.UsedSource> quellen) throws IOException {
        String extractedText = ds.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            return generateResultPdf(ds, empfehlungen, quellen);
        }

        try (PDDocument doc = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float pageWidth = PDRectangle.A4.getWidth();
            float usableWidth = pageWidth - 2 * MARGIN;

            ContentWriter writer = new ContentWriter(doc, fontRegular, fontBold, usableWidth);

            writer.writeHeading("Annotiertes Dokument: " + ds.getFileName());
            writer.writeLine("Relevante Passagen sind gelb markiert mit Kodierempfehlung-Referenz.");
            writer.writeLine("");

            // Suchbegriffe aus Empfehlungen extrahieren fuer Highlighting
            List<HighlightEntry> highlights = buildHighlights(extractedText, empfehlungen);

            // Text zeilenweise ausgeben mit Markierungen
            String[] lines = extractedText.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    writer.writeLine("");
                    continue;
                }
                // Pruefen ob diese Zeile in einer markierten Passage liegt
                String annotation = findAnnotation(line, highlights);
                if (annotation != null) {
                    writer.writeHighlightedLine(trimmed, annotation);
                } else {
                    writer.writeLine(trimmed);
                }
            }

            // Anhang: Empfehlungen
            writer.writeLine("");
            writer.writeHeading("Kodierempfehlungen (Anhang)");
            for (int i = 0; i < empfehlungen.size(); i++) {
                writer.writeHeading("Empfehlung " + (i + 1));
                for (String l : empfehlungen.get(i).split("\n")) {
                    writer.writeLine(l);
                }
                writer.writeLine("");
            }

            writer.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static class HighlightEntry {
        final String passage;
        final String annotation;
        HighlightEntry(String passage, String annotation) {
            this.passage = passage;
            this.annotation = annotation;
        }
    }

    private List<HighlightEntry> buildHighlights(String fullText, List<String> empfehlungen) {
        List<HighlightEntry> highlights = new ArrayList<>();
        String fullTextLower = fullText.toLowerCase();

        for (int i = 0; i < empfehlungen.size(); i++) {
            String empfehlung = empfehlungen.get(i);
            // Signifikante Woerter aus der Empfehlung extrahieren (>= 5 Zeichen, keine Stoppwoerter)
            String[] words = empfehlung.split("\\s+");
            for (String word : words) {
                String clean = word.replaceAll("[^a-zA-ZäöüÄÖÜß]", "").toLowerCase();
                if (clean.length() >= 6 && fullTextLower.contains(clean)) {
                    // Finde Zeilen im Originaltext die dieses Wort enthalten
                    for (String line : fullText.split("\n")) {
                        if (line.toLowerCase().contains(clean) && line.trim().length() > 10) {
                            highlights.add(new HighlightEntry(line.trim(),
                                    "Empfehlung " + (i + 1)));
                        }
                    }
                }
            }
        }
        return highlights;
    }

    private String findAnnotation(String line, List<HighlightEntry> highlights) {
        String trimmed = line.trim();
        for (HighlightEntry h : highlights) {
            if (h.passage.equals(trimmed)) {
                return h.annotation;
            }
        }
        return null;
    }

    /**
     * Hilfsklasse fuer seitenuebergreifendes PDF-Schreiben.
     */
    private static class ContentWriter {
        private final PDDocument doc;
        private final PDType1Font fontRegular;
        private final PDType1Font fontBold;
        private final float usableWidth;
        private PDPageContentStream stream;
        private float yPos;
        private static final float FONT_SIZE = 10;
        private static final float HEADING_SIZE = 13;

        ContentWriter(PDDocument doc, PDType1Font fontRegular, PDType1Font fontBold, float usableWidth) throws IOException {
            this.doc = doc;
            this.fontRegular = fontRegular;
            this.fontBold = fontBold;
            this.usableWidth = usableWidth;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) {
                stream.endText();
                stream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            yPos = PDRectangle.A4.getHeight() - MARGIN;
            stream.beginText();
            stream.setFont(fontRegular, FONT_SIZE);
            stream.newLineAtOffset(MARGIN, yPos);
        }

        private void ensureSpace(float needed) throws IOException {
            if (yPos - needed < MARGIN) {
                newPage();
            }
        }

        void writeHeading(String text) throws IOException {
            ensureSpace(HEADING_HEIGHT + LINE_HEIGHT);
            stream.setFont(fontBold, HEADING_SIZE);
            stream.newLineAtOffset(0, -HEADING_HEIGHT);
            yPos -= HEADING_HEIGHT;
            // Truncate heading if too long
            String truncated = truncateToFit(text, fontBold, HEADING_SIZE);
            stream.showText(truncated);
            stream.setFont(fontRegular, FONT_SIZE);
        }

        void writeLine(String text) throws IOException {
            ensureSpace(LINE_HEIGHT);
            stream.newLineAtOffset(0, -LINE_HEIGHT);
            yPos -= LINE_HEIGHT;
            if (text.isEmpty()) return;
            // Wortumbruch
            List<String> wrapped = wrapText(text, fontRegular, FONT_SIZE);
            stream.showText(wrapped.get(0));
            for (int i = 1; i < wrapped.size(); i++) {
                ensureSpace(LINE_HEIGHT);
                stream.newLineAtOffset(0, -LINE_HEIGHT);
                yPos -= LINE_HEIGHT;
                stream.showText(wrapped.get(i));
            }
        }

        void writeHighlightedLine(String text, String annotation) throws IOException {
            ensureSpace(LINE_HEIGHT * 2);

            // Gelbe Hintergrundfarbe
            float textWidth = Math.min(fontRegular.getStringWidth(sanitize(text)) / 1000 * FONT_SIZE, usableWidth);
            stream.endText();
            stream.setNonStrokingColor(1.0f, 1.0f, 0.6f); // Gelb
            stream.addRect(MARGIN - 2, yPos - LINE_HEIGHT - 2, textWidth + 4, LINE_HEIGHT + 2);
            stream.fill();
            stream.setNonStrokingColor(0, 0, 0); // Schwarz
            stream.beginText();
            stream.setFont(fontRegular, FONT_SIZE);
            stream.newLineAtOffset(MARGIN, yPos - LINE_HEIGHT);
            // Position korrigieren - da wir endText/beginText gemacht haben
            yPos -= LINE_HEIGHT;

            String truncated = truncateToFit(text, fontRegular, FONT_SIZE);
            stream.showText(truncated);

            // Annotation in Blau
            stream.setFont(fontBold, 8);
            stream.newLineAtOffset(0, -11);
            yPos -= 11;
            stream.setNonStrokingColor(0.0f, 0.43f, 0.78f); // Portal-Blau
            stream.showText(">> " + annotation);
            stream.setNonStrokingColor(0, 0, 0);
            stream.setFont(fontRegular, FONT_SIZE);
        }

        void close() throws IOException {
            if (stream != null) {
                stream.endText();
                stream.close();
            }
        }

        private List<String> wrapText(String text, PDType1Font font, float fontSize) throws IOException {
            List<String> lines = new ArrayList<>();
            String sanitized = sanitize(text);
            String[] words = sanitized.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String test = current.isEmpty() ? word : current + " " + word;
                float width = font.getStringWidth(test) / 1000 * fontSize;
                if (width > usableWidth && !current.isEmpty()) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(test);
                }
            }
            if (!current.isEmpty()) lines.add(current.toString());
            if (lines.isEmpty()) lines.add("");
            return lines;
        }

        private String truncateToFit(String text, PDType1Font font, float fontSize) throws IOException {
            String sanitized = sanitize(text);
            float width = font.getStringWidth(sanitized) / 1000 * fontSize;
            if (width <= usableWidth) return sanitized;
            while (sanitized.length() > 3 && font.getStringWidth(sanitized + "...") / 1000 * fontSize > usableWidth) {
                sanitized = sanitized.substring(0, sanitized.length() - 1);
            }
            return sanitized + "...";
        }

        private String sanitize(String text) {
            // PDFBox Standard14Fonts können keine speziellen Unicode-Zeichen
            return text.replace("\t", "    ")
                       .replace("\r", "")
                       .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        }
    }
}
