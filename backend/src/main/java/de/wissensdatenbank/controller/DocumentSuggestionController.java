package de.wissensdatenbank.controller;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.DocumentDto;
import de.wissensdatenbank.dto.DocumentSuggestionDto;
import de.wissensdatenbank.service.DocumentSuggestionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST-API fuer dokument-basierte KI-Kodierempfehlungen.
 */
@RestController
@RequestMapping("/api/document-suggestions")
public class DocumentSuggestionController {

    private final DocumentSuggestionService service;
    private final SecurityHelper securityHelper;

    public DocumentSuggestionController(DocumentSuggestionService service,
                                         SecurityHelper securityHelper) {
        this.service = service;
        this.securityHelper = securityHelper;
    }

    /**
     * POST /api/document-suggestions/upload
     * Laedt ein Dokument hoch fuer die KI-Kodierempfehlung.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentSuggestionDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "modelConfigId", required = false) String modelConfigId) throws IOException {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        DocumentSuggestionDto dto = service.upload(tenantId, userId, file, modelConfigId);
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/document-suggestions
     * Listet alle Dokument-Kodierempfehlungen des Mandanten.
     */
    @GetMapping
    public ResponseEntity<List<DocumentSuggestionDto>> list() {
        String tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(service.list(tenantId));
    }

    /**
     * GET /api/document-suggestions/{id}
     * Gibt eine einzelne Dokument-Kodierempfehlung zurueck.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentSuggestionDto> getById(@PathVariable Long id) {
        String tenantId = securityHelper.getCurrentTenantId();
        return ResponseEntity.ok(service.getById(tenantId, id));
    }

    /**
     * POST /api/document-suggestions/{id}/start
     * Startet die KI-Kodierempfehlung fuer ein hochgeladenes Dokument.
     */
    @PostMapping("/{id}/start")
    public ResponseEntity<Void> start(@PathVariable Long id) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        String jwtToken = securityHelper.getCurrentToken();
        service.startSuggestion(id, tenantId, userId, jwtToken);
        return ResponseEntity.accepted().build();
    }

    /**
     * GET /api/document-suggestions/{id}/pdf
     * Gibt das Kodierergebnis als PDF-Download zurueck.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadResultPdf(@PathVariable Long id) throws IOException {
        String tenantId = securityHelper.getCurrentTenantId();
        byte[] pdf = service.generateResultPdf(tenantId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"kodierempfehlung-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /**
     * GET /api/document-suggestions/{id}/annotated-pdf
     * Gibt das annotierte Originaldokument als PDF mit gelb markierten Passagen zurueck.
     */
    @GetMapping("/{id}/annotated-pdf")
    public ResponseEntity<byte[]> downloadAnnotatedPdf(@PathVariable Long id) {
        String tenantId = securityHelper.getCurrentTenantId();
        try {
            byte[] pdf = service.generateAnnotatedPdf(tenantId, id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"annotiert-" + id + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            // Fallback: regulaeres Ergebnis-PDF wenn annotiertes fehlschlaegt
            try {
                byte[] pdf = service.generateResultPdf(tenantId, id);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"kodierempfehlung-" + id + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(pdf);
            } catch (Exception e2) {
                return ResponseEntity.internalServerError().build();
            }
        }
    }

    /**
     * POST /api/document-suggestions/{id}/create-document
     * Erzeugt ein neues Dokument in der Wissensdatenbank aus dem Kodierergebnis.
     */
    @PostMapping("/{id}/create-document")
    public ResponseEntity<DocumentDto> createDocument(@PathVariable Long id) {
        String tenantId = securityHelper.getCurrentTenantId();
        DocumentDto doc = service.createDocumentFromSuggestion(tenantId, id);
        return ResponseEntity.ok(doc);
    }

    /**
     * DELETE /api/document-suggestions/{id}
     * Loescht eine Dokument-Kodierempfehlung.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String tenantId = securityHelper.getCurrentTenantId();
        service.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
