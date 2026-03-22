package de.wissensdatenbank.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extrahiert Rohtext aus einem SEG4-PDF.
 * Keine Businesslogik – nur PDF → String.
 */
@Component
public class Seg4PdfParser {

    private static final Logger log = LoggerFactory.getLogger(Seg4PdfParser.class);

    /**
     * Liest ein PDF aus dem InputStream und gibt den kompletten Text zurück.
     *
     * @param inputStream PDF-Datenstrom
     * @return extrahierter Rohtext
     * @throws Seg4ParseException bei Lese-/Parse-Fehlern
     */
    public String extractText(InputStream inputStream) {
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.debug("PDF-Text extrahiert: {} Zeichen", text.length());
            return text;
        } catch (IOException e) {
            throw new Seg4ParseException("PDF konnte nicht gelesen werden: " + e.getMessage(), e);
        }
    }
}
