package de.wissensdatenbank.controller;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.HandbuchImportRequest;
import de.wissensdatenbank.service.HandbuchImportService;
import de.wissensdatenbank.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST-Endpunkt fuer den Bulk-Import eines Benutzerhandbuchs.
 */
@RestController
@RequestMapping("/api/handbuch-import")
public class HandbuchImportController {

    private final HandbuchImportService importService;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public HandbuchImportController(HandbuchImportService importService,
                                     SecurityHelper securityHelper,
                                     PermissionService permissionService) {
        this.importService = importService;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    /**
     * POST /api/handbuch-import
     * Importiert ein komplettes Benutzerhandbuch (JSON-Format aus parse_handbuch.py).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> importHandbuch(
            @RequestBody HandbuchImportRequest request) {
        permissionService.requireSchreiben();
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();

        var result = importService.importHandbuch(tenantId, userId, request);

        return ResponseEntity.ok(Map.of(
                "productId", result.productId(),
                "productVersionId", result.productVersionId(),
                "knowledgeItemCount", result.knowledgeItemCount(),
                "subArticleCount", result.subArticleCount(),
                "message", String.format("Import erfolgreich: %d Kapitel mit %d Abschnitten",
                        result.knowledgeItemCount(), result.subArticleCount())
        ));
    }
}
