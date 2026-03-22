package de.wissensdatenbank.service;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.Seg4Recommendation;
import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.enums.KnowledgeType;
import de.wissensdatenbank.parser.*;
import de.wissensdatenbank.repository.KnowledgeItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestriert den SEG4-PDF-Import:
 * PDF → Text → Blöcke → Felder → Normalisierung → Persistierung.
 */
@Service
public class Seg4ImportService {

    private static final Logger log = LoggerFactory.getLogger(Seg4ImportService.class);

    private final Seg4PdfParser pdfParser;
    private final Seg4BlockDetector blockDetector;
    private final Seg4FieldExtractor fieldExtractor;
    private final Seg4Normalizer normalizer;
    private final KnowledgeItemRepository knowledgeItemRepository;

    public Seg4ImportService(Seg4PdfParser pdfParser,
                             Seg4BlockDetector blockDetector,
                             Seg4FieldExtractor fieldExtractor,
                             Seg4Normalizer normalizer,
                             KnowledgeItemRepository knowledgeItemRepository) {
        this.pdfParser = pdfParser;
        this.blockDetector = blockDetector;
        this.fieldExtractor = fieldExtractor;
        this.normalizer = normalizer;
        this.knowledgeItemRepository = knowledgeItemRepository;
    }

    /**
     * Importiert ein SEG4-PDF und speichert alle erkannten Kodierempfehlungen.
     *
     * @param tenantId  Mandant
     * @param userId    importierender Benutzer
     * @param fileName  Dateiname (für Referenz)
     * @param pdfStream PDF-Datenstrom
     * @return das gespeicherte KnowledgeItem mit allen Empfehlungen
     */
    @Transactional
    public KnowledgeItem importPdf(String tenantId, String userId,
                                    String fileName, InputStream pdfStream) {
        // 1. PDF → Text
        String rawText = pdfParser.extractText(pdfStream);

        // 2. Text → Blöcke
        List<Seg4RawBlock> blocks = blockDetector.detectBlocks(rawText);
        if (blocks.isEmpty()) {
            throw new Seg4ParseException("Keine Kodierempfehlungen im PDF gefunden: " + fileName);
        }

        // 3. Blöcke → Felder → Normalisierung
        List<Seg4ParsedFields> parsedList = blocks.stream()
                .map(fieldExtractor::extract)
                .map(normalizer::normalize)
                .toList();

        // 4. KnowledgeItem erstellen
        KnowledgeItem item = new KnowledgeItem();
        item.setTenantId(tenantId);
        item.setTitle("SEG4 Import: " + fileName);
        item.setKnowledgeType(KnowledgeType.SEG4);
        item.setBindingLevel(determineBindingLevel(parsedList));
        item.setCreatedBy(userId);
        item.setUpdatedBy(userId);
        item.setSourceReference(fileName);
        item.setKeywords(collectKeywords(parsedList));

        // 5. Empfehlungen zuordnen
        List<Seg4Recommendation> recommendations = new ArrayList<>();
        for (Seg4ParsedFields parsed : parsedList) {
            Seg4Recommendation rec = toEntity(parsed);
            rec.setKnowledgeItem(item);
            recommendations.add(rec);
        }
        item.setSeg4Recommendations(recommendations);

        KnowledgeItem saved = knowledgeItemRepository.save(item);
        log.info("SEG4-Import abgeschlossen: {} Empfehlungen aus '{}' fuer Mandant {}",
                parsedList.size(), fileName, tenantId);
        return saved;
    }

    private BindingLevel determineBindingLevel(List<Seg4ParsedFields> list) {
        boolean hasArbitration = list.stream().anyMatch(Seg4ParsedFields::isArbitration);
        return hasArbitration ? BindingLevel.LEX_SPECIALIS : BindingLevel.EMPFEHLUNG;
    }

    private String collectKeywords(List<Seg4ParsedFields> list) {
        return list.stream()
                .map(Seg4ParsedFields::getSchlagworte)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }

    private Seg4Recommendation toEntity(Seg4ParsedFields parsed) {
        Seg4Recommendation rec = new Seg4Recommendation();
        rec.setRecommendationNumber(parsed.getRecommendationNumber());
        rec.setSchlagworte(parsed.getSchlagworte());
        rec.setErstelltAm(parsed.getErstelltAm());
        rec.setAktualisiertAm(parsed.getAktualisiertAm());
        rec.setProblemErlaeuterung(parsed.getProblemErlaeuterung());
        rec.setEmpfehlung(parsed.getEmpfehlung());
        rec.setEntscheidung(parsed.getEntscheidung());
        rec.setZusatzhinweis(parsed.getZusatzhinweis());
        rec.setArbitration(parsed.isArbitration());
        rec.setOriginalText(parsed.getOriginalText());
        return rec;
    }
}
