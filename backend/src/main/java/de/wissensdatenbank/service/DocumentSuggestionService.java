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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Verwaltet dokument-basierte KI-Kodierempfehlungen.
 * Dateien werden hochgeladen, Text extrahiert und asynchron kodiert.
 */
@Service
public class DocumentSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSuggestionService.class);

    private final DocumentSuggestionRepository repository;
    private final SuggestionService suggestionService;
    private final ObjectMapper objectMapper;

    public DocumentSuggestionService(DocumentSuggestionRepository repository,
                                      SuggestionService suggestionService,
                                      ObjectMapper objectMapper) {
        this.repository = repository;
        this.suggestionService = suggestionService;
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
