package de.wissensdatenbank.controller;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.KnowledgeItemDto;
import de.wissensdatenbank.enums.KnowledgeType;
import de.wissensdatenbank.service.KnowledgeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST-API für Wissensobjekte (KnowledgeItems).
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final SecurityHelper securityHelper;

    public KnowledgeController(KnowledgeService knowledgeService, SecurityHelper securityHelper) {
        this.knowledgeService = knowledgeService;
        this.securityHelper = securityHelper;
    }

    @GetMapping
    public Page<KnowledgeItemDto> list(
            @RequestParam(required = false) KnowledgeType type,
            Pageable pageable) {
        String tenantId = securityHelper.getCurrentTenantId();
        if (type != null) {
            return knowledgeService.findByType(tenantId, type, pageable);
        }
        return knowledgeService.findAll(tenantId, pageable);
    }

    @GetMapping("/{id}")
    public KnowledgeItemDto getById(@PathVariable Long id) {
        String tenantId = securityHelper.getCurrentTenantId();
        return knowledgeService.findDtoById(tenantId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        String tenantId = securityHelper.getCurrentTenantId();
        knowledgeService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
