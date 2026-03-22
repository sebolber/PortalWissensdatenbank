package de.wissensdatenbank.service;

import de.wissensdatenbank.dto.SuggestionResponse;
import de.wissensdatenbank.entity.DocumentSuggestion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Erzeugt PDF-Dokumente aus Kodierempfehlungen.
 * Nutzt ausschliesslich PDFBox Standard14Fonts (WinAnsiEncoding).
 */
@Service
public class SuggestionPdfService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionPdfService.class);

    private static final float MARGIN = 50;
    private static final float FONT_SIZE = 10;
    private static final float HEADING_SIZE = 13;
    private static final float LINE_HEIGHT = 14;
    private static final float HEADING_GAP = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public byte[] generateResultPdf(DocumentSuggestion ds, List<String> empfehlungen,
                                     List<SuggestionResponse.UsedSource> quellen) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PdfWriter w = new PdfWriter(doc);

            w.heading("KI-Kodierempfehlung: " + ds.getFileName());
            w.line("");
            w.line("Erstellt: " + (ds.getCreatedAt() != null ? ds.getCreatedAt().format(DATE_FMT) : "-"));
            if (ds.getLlmModel() != null) {
                w.line("Modell: " + ds.getLlmModel() + "  |  Tokens: " + ds.getTokenCount());
            }
            w.line("");

            for (int i = 0; i < empfehlungen.size(); i++) {
                w.heading("Empfehlung " + (i + 1));
                for (String line : empfehlungen.get(i).split("\n")) {
                    w.line(line);
                }
                w.line("");
            }

            if (quellen != null && !quellen.isEmpty()) {
                w.heading("Verwendete Quellen");
                for (SuggestionResponse.UsedSource q : quellen) {
                    w.line("- " + q.title() + " [" + q.bindingLevel() + "] " + q.matchReason());
                }
            }

            w.finish();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    public byte[] generateAnnotatedPdf(DocumentSuggestion ds, List<String> empfehlungen,
                                        List<SuggestionResponse.UsedSource> quellen) throws IOException {
        String extractedText = ds.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            return generateResultPdf(ds, empfehlungen, quellen);
        }

        try (PDDocument doc = new PDDocument()) {
            PdfWriter w = new PdfWriter(doc);

            w.heading("Annotiertes Dokument: " + ds.getFileName());
            w.line("Relevante Passagen sind gelb markiert mit Kodierempfehlung-Referenz.");
            w.line("");

            Set<String> highlightedLines = buildHighlightedLines(extractedText, empfehlungen);

            for (String line : extractedText.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    w.line("");
                    continue;
                }
                String annotation = highlightedLines.contains(trimmed)
                        ? findAnnotation(trimmed, extractedText, empfehlungen) : null;
                if (annotation != null) {
                    w.highlightedLine(trimmed, annotation);
                } else {
                    w.line(trimmed);
                }
            }

            w.line("");
            w.heading("Kodierempfehlungen (Anhang)");
            for (int i = 0; i < empfehlungen.size(); i++) {
                w.heading("Empfehlung " + (i + 1));
                for (String l : empfehlungen.get(i).split("\n")) {
                    w.line(l);
                }
                w.line("");
            }

            w.finish();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private Set<String> buildHighlightedLines(String fullText, List<String> empfehlungen) {
        Set<String> matched = new LinkedHashSet<>();
        String fullLower = fullText.toLowerCase();
        String[] textLines = fullText.split("\n");

        for (String empfehlung : empfehlungen) {
            for (String word : empfehlung.split("\\s+")) {
                String clean = word.replaceAll("[^a-zA-ZaeoeueAeOeUess]", "").toLowerCase();
                if (clean.length() >= 6 && fullLower.contains(clean)) {
                    for (String tl : textLines) {
                        String trimmed = tl.trim();
                        if (trimmed.length() > 10 && trimmed.toLowerCase().contains(clean)) {
                            matched.add(trimmed);
                        }
                    }
                }
            }
        }
        return matched;
    }

    private String findAnnotation(String line, String fullText, List<String> empfehlungen) {
        String lineLower = line.toLowerCase();
        for (int i = 0; i < empfehlungen.size(); i++) {
            for (String word : empfehlungen.get(i).split("\\s+")) {
                String clean = word.replaceAll("[^a-zA-ZaeoeueAeOeUess]", "").toLowerCase();
                if (clean.length() >= 6 && lineLower.contains(clean)) {
                    return "Empfehlung " + (i + 1);
                }
            }
        }
        return "Empfehlung";
    }

    /**
     * Robuster PDF-Writer ohne gemischten Text/Grafik-Zustand.
     * Jede Zeile wird als eigener beginText/endText-Block geschrieben.
     */
    private static class PdfWriter {
        private final PDDocument doc;
        private final PDType1Font fontRegular;
        private final PDType1Font fontBold;
        private final float usableWidth;
        private PDPageContentStream cs;
        private float y;

        PdfWriter(PDDocument doc) throws IOException {
            this.doc = doc;
            this.fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            this.fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            this.usableWidth = PDRectangle.A4.getWidth() - 2 * MARGIN;
            newPage();
        }

        private void newPage() throws IOException {
            if (cs != null) cs.close();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < MARGIN) {
                newPage();
            }
        }

        void heading(String text) throws IOException {
            ensureSpace(HEADING_GAP + LINE_HEIGHT);
            y -= HEADING_GAP;
            String safe = sanitize(text);
            cs.beginText();
            cs.setFont(fontBold, HEADING_SIZE);
            cs.newLineAtOffset(MARGIN, y);
            cs.showText(safe);
            cs.endText();
        }

        void line(String text) throws IOException {
            if (text == null || text.isEmpty()) {
                y -= LINE_HEIGHT;
                return;
            }
            String safe = sanitize(text);
            List<String> wrapped = wrapText(safe, fontRegular, FONT_SIZE);
            for (String part : wrapped) {
                ensureSpace(LINE_HEIGHT);
                y -= LINE_HEIGHT;
                cs.beginText();
                cs.setFont(fontRegular, FONT_SIZE);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(part);
                cs.endText();
            }
        }

        void highlightedLine(String text, String annotation) throws IOException {
            String safe = sanitize(text);
            List<String> wrapped = wrapText(safe, fontRegular, FONT_SIZE);

            for (String part : wrapped) {
                ensureSpace(LINE_HEIGHT);
                y -= LINE_HEIGHT;

                // Gelber Hintergrund
                float tw;
                try {
                    tw = Math.min(fontRegular.getStringWidth(part) / 1000f * FONT_SIZE, usableWidth);
                } catch (Exception e) {
                    tw = usableWidth;
                }
                cs.setNonStrokingColor(1.0f, 1.0f, 0.6f);
                cs.addRect(MARGIN - 2, y - 2, tw + 4, LINE_HEIGHT);
                cs.fill();

                // Text schwarz
                cs.setNonStrokingColor(0f, 0f, 0f);
                cs.beginText();
                cs.setFont(fontRegular, FONT_SIZE);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText(part);
                cs.endText();
            }

            // Annotation in Blau
            ensureSpace(LINE_HEIGHT);
            y -= 11;
            cs.setNonStrokingColor(0f, 0.43f, 0.78f);
            cs.beginText();
            cs.setFont(fontBold, 8);
            cs.newLineAtOffset(MARGIN + 4, y);
            cs.showText(">> " + sanitize(annotation));
            cs.endText();
            cs.setNonStrokingColor(0f, 0f, 0f);
        }

        void finish() throws IOException {
            if (cs != null) cs.close();
        }

        private List<String> wrapText(String text, PDType1Font font, float fontSize) {
            List<String> lines = new ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                String test = current.isEmpty() ? word : current + " " + word;
                float width;
                try {
                    width = font.getStringWidth(test) / 1000f * fontSize;
                } catch (Exception e) {
                    width = test.length() * 5f; // Fallback
                }
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

        /**
         * Ersetzt Zeichen die nicht in WinAnsiEncoding darstellbar sind.
         */
        private static String sanitize(String text) {
            if (text == null) return "";
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\t') { sb.append("    "); continue; }
                if (c == '\r') continue;
                // WinAnsiEncoding: 0x20-0x7E (ASCII printable) + 0xA0-0xFF (Latin supplement)
                // Plus some Windows-1252 extras in 0x80-0x9F range
                if (c >= 0x20 && c <= 0x7E) { sb.append(c); continue; }
                if (c >= 0xA0 && c <= 0xFF) { sb.append(c); continue; }
                // Windows-1252 mapped characters
                switch (c) {
                    case '\u2013': sb.append('-'); break;  // en-dash
                    case '\u2014': sb.append('-'); break;  // em-dash
                    case '\u2018': case '\u2019': sb.append('\''); break; // smart quotes
                    case '\u201C': case '\u201D': sb.append('"'); break;  // smart double quotes
                    case '\u2022': sb.append('*'); break;  // bullet
                    case '\u2026': sb.append("..."); break; // ellipsis
                    case '\u20AC': sb.append("EUR"); break; // euro sign
                    default:
                        if (c > 0xFF) {
                            sb.append('?');
                        } else {
                            sb.append(c);
                        }
                }
            }
            return sb.toString();
        }
    }
}
