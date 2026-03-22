package de.wissensdatenbank.controller;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.service.Seg4ImportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * REST-Endpunkt für den SEG4-PDF-Import.
 */
@RestController
@RequestMapping("/api/seg4")
public class Seg4ImportController {

    private final Seg4ImportService importService;
    private final SecurityHelper securityHelper;

    public Seg4ImportController(Seg4ImportService importService,
                                 SecurityHelper securityHelper) {
        this.importService = importService;
        this.securityHelper = securityHelper;
    }

    /**
     * POST /api/seg4/import
     * Multipart-Upload eines SEG4-PDF.
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importPdf(
            @RequestParam("file") MultipartFile file) throws IOException {

        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        String fileName = file.getOriginalFilename();

        KnowledgeItem item = importService.importPdf(
                tenantId, userId, fileName, file.getInputStream());

        return ResponseEntity.ok(Map.of(
                "id", item.getId(),
                "title", item.getTitle(),
                "recommendationCount", item.getSeg4Recommendations().size(),
                "message", "SEG4-Import erfolgreich"
        ));
    }
}
