package de.wissensdatenbank.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.wissensdatenbank.dto.DocumentSuggestionDto;
import de.wissensdatenbank.dto.SuggestionRequest;
import de.wissensdatenbank.dto.SuggestionResponse;
import de.wissensdatenbank.entity.DocumentSuggestion;
import de.wissensdatenbank.repository.DocumentSuggestionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import de.wissensdatenbank.dto.DocumentCreateRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Verwaltet dokument-basierte KI-Kodierempfehlungen.
 * Dateien werden hochgeladen, Text extrahiert und asynchron kodiert.
 */
@Service
@Transactional(readOnly = true)
public class DocumentSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSuggestionService.class);

    private final DocumentSuggestionRepository repository;
    private final SuggestionService suggestionService;
    private final DocumentService documentService;
    private final SuggestionPdfService pdfService;
    private final ObjectMapper objectMapper;

    public DocumentSuggestionService(DocumentSuggestionRepository repository,
                                      SuggestionService suggestionService,
                                      DocumentService documentService,
                                      SuggestionPdfService pdfService,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.suggestionService = suggestionService;
        this.documentService = documentService;
        this.pdfService = pdfService;
        this.objectMapper = objectMapper;
    }

    /**
     * Laedt eine Datei hoch und speichert sie als PENDING.
     */
    @Transactional
    public DocumentSuggestionDto upload(String tenantId, String userId, MultipartFile file,
                                         String modelConfigId) throws IOException {
        DocumentSuggestion ds = new DocumentSuggestion();
        ds.setTenantId(tenantId);
        ds.setUserId(userId);
        ds.setFileName(file.getOriginalFilename());
        ds.setFileContentType(file.getContentType());
        ds.setFileData(file.getBytes());
        ds.setModelConfigId(modelConfigId);
        ds.setStatus("PENDING");

        // Text sofort extrahieren
        try {
            String text = extractText(file);
            ds.setExtractedText(text);
        } catch (Exception e) {
            log.warn("Text-Extraktion fehlgeschlagen fuer {}: {}", file.getOriginalFilename(), e.getMessage());
            ds.setStatus("ERROR");
            ds.setErrorMessage("Text-Extraktion fehlgeschlagen: " + e.getMessage());
        }

        ds = repository.save(ds);
        return toDto(ds);
    }

    /**
     * Listet alle Dokument-Kodierempfehlungen eines Mandanten.
     */
    @Transactional(readOnly = true)
    public List<DocumentSuggestionDto> list(String tenantId) {
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Gibt eine einzelne Dokument-Kodierempfehlung zurueck.
     */
    @Transactional(readOnly = true)
    public DocumentSuggestionDto getById(String tenantId, Long id) {
        return repository.findByIdAndTenantId(id, tenantId)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Dokument-Kodierempfehlung nicht gefunden: " + id));
    }

    /**
     * Loescht eine Dokument-Kodierempfehlung.
     */
    @Transactional
    public void delete(String tenantId, Long id) {
        DocumentSuggestion ds = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Dokument-Kodierempfehlung nicht gefunden: " + id));
        repository.delete(ds);
    }

    /**
     * Startet die KI-Kodierempfehlung asynchron.
     */
    @Async
    @Transactional
    public void startSuggestion(Long id, String tenantId, String userId, String jwtToken) {
        DocumentSuggestion ds = repository.findByIdAndTenantId(id, tenantId).orElse(null);
        if (ds == null) {
            log.warn("DocumentSuggestion {} nicht gefunden fuer Mandant {}", id, tenantId);
            return;
        }

        if (ds.getExtractedText() == null || ds.getExtractedText().isBlank()) {
            ds.setStatus("ERROR");
            ds.setErrorMessage("Kein Text aus dem Dokument extrahiert.");
            repository.save(ds);
            return;
        }

        ds.setStatus("PROCESSING");
        repository.save(ds);

        try {
            SuggestionRequest request = new SuggestionRequest(
                    ds.getExtractedText(),
                    List.of(),
                    List.of(),
                    ds.getModelConfigId()
            );

            SuggestionResponse response = suggestionService.generateSuggestion(
                    tenantId, userId, jwtToken, request);

            ds.setEmpfehlungen(String.join("\n===EMPFEHLUNG===\n", response.empfehlungen()));
            ds.setLlmModel(response.llmModel());
            ds.setTokenCount(response.tokenCount());
            ds.setAuditLogId(response.auditLogId());
            ds.setStatus("COMPLETED");

            try {
                ds.setQuellenJson(objectMapper.writeValueAsString(response.quellen()));
            } catch (JsonProcessingException e) {
                log.warn("Quellen konnten nicht serialisiert werden", e);
            }

            repository.save(ds);
            log.info("Dokument-Kodierempfehlung {} fuer Mandant {} abgeschlossen", id, tenantId);

        } catch (Exception e) {
            log.error("Fehler bei Dokument-Kodierempfehlung {} fuer Mandant {}", id, tenantId, e);
            ds.setStatus("ERROR");
            ds.setErrorMessage(e.getMessage());
            repository.save(ds);
        }
    }

    /**
     * Erzeugt ein PDF mit den Kodierempfehlungen.
     */
    @Transactional(readOnly = true)
    public byte[] generateResultPdf(String tenantId, Long id) throws IOException {
        DocumentSuggestion ds = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Nicht gefunden: " + id));
        DocumentSuggestionDto dto = toDto(ds);
        return pdfService.generateResultPdf(ds, dto.empfehlungen(), dto.quellen());
    }

    /**
     * Erzeugt ein annotiertes PDF des Originaldokuments mit gelb markierten Passagen.
     */
    @Transactional(readOnly = true)
    public byte[] generateAnnotatedPdf(String tenantId, Long id) throws IOException {
        DocumentSuggestion ds = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Nicht gefunden: " + id));
        DocumentSuggestionDto dto = toDto(ds);
        return pdfService.generateAnnotatedPdf(ds, dto.empfehlungen(), dto.quellen());
    }

    /**
     * Erzeugt ein neues Dokument in der Wissensdatenbank aus dem hochgeladenen Dokument
     * mit annotierten Kodierempfehlungen.
     */
    @Transactional
    public de.wissensdatenbank.dto.DocumentDto createDocumentFromSuggestion(String tenantId, Long id) {
        DocumentSuggestion ds = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Nicht gefunden: " + id));

        if (!"COMPLETED".equals(ds.getStatus())) {
            throw new IllegalStateException("Kodierempfehlung ist noch nicht abgeschlossen.");
        }

        DocumentSuggestionDto dto = toDto(ds);

        // Inhalt: Originaltext mit markierten Passagen und Empfehlungen
        StringBuilder content = new StringBuilder();
        content.append("# Originaldokument: ").append(ds.getFileName()).append("\n\n");
        content.append(ds.getExtractedText()).append("\n\n");
        content.append("---\n\n");
        content.append("# KI-Kodierempfehlungen\n\n");
        for (int i = 0; i < dto.empfehlungen().size(); i++) {
            content.append("## Empfehlung ").append(i + 1).append("\n\n");
            content.append(dto.empfehlungen().get(i)).append("\n\n");
        }

        if (dto.quellen() != null && !dto.quellen().isEmpty()) {
            content.append("## Verwendete Quellen\n\n");
            for (SuggestionResponse.UsedSource q : dto.quellen()) {
                content.append("- **").append(q.title()).append("** [").append(q.bindingLevel()).append("] ")
                       .append(q.matchReason()).append("\n");
            }
        }

        // Zusammenfassung aus erster Empfehlung (KURZFAZIT)
        String summary = "";
        if (!dto.empfehlungen().isEmpty()) {
            String first = dto.empfehlungen().get(0);
            int kurzfazitIdx = first.indexOf("KURZFAZIT:");
            if (kurzfazitIdx >= 0) {
                int endIdx = first.indexOf("\n", kurzfazitIdx);
                summary = first.substring(kurzfazitIdx + "KURZFAZIT:".length(),
                        endIdx > 0 ? endIdx : Math.min(first.length(), kurzfazitIdx + 200)).trim();
            }
            if (summary.isEmpty()) {
                summary = first.substring(0, Math.min(first.length(), 200)).trim();
            }
        }

        DocumentCreateRequest createReq = new DocumentCreateRequest(
                "Kodierempfehlung: " + ds.getFileName(),
                content.toString(),
                summary.length() > 2000 ? summary.substring(0, 2000) : summary,
                null,
                List.of(),
                true
        );

        return documentService.create(createReq);
    }

    private String extractText(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType != null && contentType.contains("pdf")) {
            try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }
        // Fuer Textdateien (txt, csv, etc.) direkt lesen
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private DocumentSuggestionDto toDto(DocumentSuggestion ds) {
        List<String> empfehlungenList = List.of();
        if (ds.getEmpfehlungen() != null && !ds.getEmpfehlungen().isBlank()) {
            empfehlungenList = Arrays.stream(ds.getEmpfehlungen().split("===EMPFEHLUNG==="))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }

        List<SuggestionResponse.UsedSource> quellen = List.of();
        if (ds.getQuellenJson() != null && !ds.getQuellenJson().isBlank()) {
            try {
                quellen = objectMapper.readValue(ds.getQuellenJson(),
                        new TypeReference<List<SuggestionResponse.UsedSource>>() {});
            } catch (JsonProcessingException e) {
                log.warn("Quellen konnten nicht deserialisiert werden fuer DS {}", ds.getId());
            }
        }

        return new DocumentSuggestionDto(
                ds.getId(),
                ds.getFileName(),
                ds.getFileContentType(),
                ds.getStatus(),
                ds.getErrorMessage(),
                empfehlungenList,
                ds.getLlmModel(),
                ds.getTokenCount(),
                quellen,
                ds.getAuditLogId(),
                ds.getModelConfigId(),
                ds.getCreatedAt(),
                ds.getUpdatedAt()
        );
    }
}
