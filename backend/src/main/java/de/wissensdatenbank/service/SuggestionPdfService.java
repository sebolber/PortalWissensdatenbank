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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

            Map<String, String> lineAnnotations = buildLineAnnotations(extractedText, empfehlungen);

            // Zeilen blockweise verarbeiten: zusammenhaengende markierte Zeilen
            // bekommen nur EINMAL am Ende des Blocks den Kommentar
            String[] lines = extractedText.split("\n");
            String currentBlockAnnotation = null;
            List<String> currentBlock = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();
                String annotation = trimmed.isEmpty() ? null : lineAnnotations.get(trimmed);

                if (annotation != null) {
                    // Zeile gehoert zu einem markierten Block
                    if (currentBlockAnnotation == null) {
                        currentBlockAnnotation = annotation;
                    }
                    currentBlock.add(trimmed);
                } else {
                    // Block beenden falls einer offen ist
                    if (!currentBlock.isEmpty()) {
                        for (String blockLine : currentBlock) {
                            w.highlightedLineNoAnnotation(blockLine);
                        }
                        w.annotationComment(currentBlockAnnotation);
                        currentBlock.clear();
                        currentBlockAnnotation = null;
                    }
                    // Normale Zeile ausgeben
                    w.line(trimmed);
                }
            }
            // Letzten offenen Block abschliessen
            if (!currentBlock.isEmpty()) {
                for (String blockLine : currentBlock) {
                    w.highlightedLineNoAnnotation(blockLine);
                }
                w.annotationComment(currentBlockAnnotation);
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

    /**
     * Ordnet jeder Textzeile die zugehoerige Empfehlung zu (oder null).
     * Gibt eine Map zurueck: trimmed line -> "Empfehlung X".
     */
    private Map<String, String> buildLineAnnotations(String fullText, List<String> empfehlungen) {
        Map<String, String> result = new LinkedHashMap<>();
        String[] textLines = fullText.split("\n");

        for (int i = 0; i < empfehlungen.size(); i++) {
            // Signifikante Woerter aus dieser Empfehlung sammeln
            Set<String> keywords = new LinkedHashSet<>();
            for (String word : empfehlungen.get(i).split("\\s+")) {
                String clean = word.replaceAll("[^a-zA-Z\u00e4\u00f6\u00fc\u00c4\u00d6\u00dc\u00df]", "").toLowerCase();
                if (clean.length() >= 6) {
                    keywords.add(clean);
                }
            }

            String label = "Empfehlung " + (i + 1);
            for (String tl : textLines) {
                String trimmed = tl.trim();
                if (trimmed.length() <= 10 || result.containsKey(trimmed)) continue;
                String lower = trimmed.toLowerCase();
                for (String kw : keywords) {
                    if (lower.contains(kw)) {
                        result.put(trimmed, label);
                        break;
                    }
                }
            }
        }
        return result;
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
            safeShowText(safe);
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
                safeShowText(part);
                cs.endText();
            }
        }

        /** Gelb markierte Zeile OHNE Annotation-Kommentar. */
        void highlightedLineNoAnnotation(String text) throws IOException {
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
                safeShowText(part);
                cs.endText();
            }
        }

        /** Blauer Kommentar am Ende eines markierten Blocks. */
        void annotationComment(String annotation) throws IOException {
            if (annotation == null) return;
            ensureSpace(LINE_HEIGHT);
            y -= 11;
            cs.setNonStrokingColor(0f, 0.43f, 0.78f);
            cs.beginText();
            cs.setFont(fontBold, 8);
            cs.newLineAtOffset(MARGIN + 4, y);
            safeShowText(">> " + sanitize(annotation));
            cs.endText();
            cs.setNonStrokingColor(0f, 0f, 0f);
            y -= 4; // kleiner Abstand nach dem Kommentar
        }

        private void safeShowText(String text) throws IOException {
            try {
                cs.showText(text);
            } catch (Exception e) {
                // Aggressiver Fallback: nur ASCII behalten
                String ascii = text.replaceAll("[^\\x20-\\x7E]", "?");
                try {
                    cs.showText(ascii);
                } catch (Exception e2) {
                    cs.showText("(Darstellungsfehler)");
                }
            }
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
                    case '\u0080': case '\u0081': case '\u0082': case '\u0083':
                    case '\u0084': case '\u0085': case '\u0086': case '\u0087':
                    case '\u0088': case '\u0089': case '\u008A': case '\u008B':
                    case '\u008C': case '\u008D': case '\u008E': case '\u008F':
                    case '\u0090': case '\u0091': case '\u0092': case '\u0093':
                    case '\u0094': case '\u0095': case '\u0096': case '\u0097':
                    case '\u0098': case '\u0099': case '\u009A': case '\u009B':
                    case '\u009C': case '\u009D': case '\u009E': case '\u009F':
                        sb.append(' '); break; // C1 control chars - nicht in WinAnsi
                    case '\u2013': sb.append('-'); break;  // en-dash
                    case '\u2014': sb.append('-'); break;  // em-dash
                    case '\u2018': case '\u2019': sb.append('\''); break; // smart quotes
                    case '\u201C': case '\u201D': sb.append('"'); break;  // smart double quotes
                    case '\u2022': sb.append('*'); break;  // bullet
                    case '\u2026': sb.append("..."); break; // ellipsis
                    case '\u20AC': sb.append("EUR"); break; // euro sign
                    case '\u2010': case '\u2011': case '\u2012': sb.append('-'); break; // hyphens
                    case '\u2039': sb.append('<'); break;  // single angle quote
                    case '\u203A': sb.append('>'); break;
                    case '\u2002': case '\u2003': case '\u2009': case '\u200A':
                    case '\u200B': case '\u00AD':
                        sb.append(' '); break; // various spaces
                    default:
                        sb.append(' '); // alles andere durch Leerzeichen ersetzen
                }
            }
            return sb.toString();
        }
    }
}
