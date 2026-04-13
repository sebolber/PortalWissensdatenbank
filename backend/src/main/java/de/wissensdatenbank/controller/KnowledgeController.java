package de.wissensdatenbank.controller;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.KnowledgeItemCreateRequest;
import de.wissensdatenbank.dto.KnowledgeItemDto;
import de.wissensdatenbank.dto.KnowledgeSubArticleTreeDto;
import de.wissensdatenbank.enums.KnowledgeType;
import de.wissensdatenbank.service.KnowledgeService;
import de.wissensdatenbank.service.PermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-API für Wissensobjekte (KnowledgeItems).
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public KnowledgeController(KnowledgeService knowledgeService, SecurityHelper securityHelper,
                               PermissionService permissionService) {
        this.knowledgeService = knowledgeService;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    @GetMapping
    public Page<KnowledgeItemDto> list(
            @RequestParam(required = false) KnowledgeType type,
            Pageable pageable) {
        permissionService.requireLesen();
        String tenantId = securityHelper.getCurrentTenantId();
        if (type != null) {
            return knowledgeService.findByType(tenantId, type, pageable);
        }
        return knowledgeService.findAll(tenantId, pageable);
    }

    @GetMapping("/{id}")
    public KnowledgeItemDto getById(@PathVariable Long id) {
        permissionService.requireLesen();
        String tenantId = securityHelper.getCurrentTenantId();
        return knowledgeService.findDtoById(tenantId, id);
    }

    @PostMapping
    public ResponseEntity<KnowledgeItemDto> create(@RequestBody KnowledgeItemCreateRequest request) {
        permissionService.requireSchreiben();
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        KnowledgeItemDto created = knowledgeService.create(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public KnowledgeItemDto update(@PathVariable Long id, @RequestBody KnowledgeItemCreateRequest request) {
        permissionService.requireSchreiben();
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        return knowledgeService.update(tenantId, userId, id, request);
    }

    @GetMapping("/{id}/sections")
    public List<KnowledgeSubArticleTreeDto> getSubArticleTree(@PathVariable Long id) {
        permissionService.requireLesen();
        String tenantId = securityHelper.getCurrentTenantId();
        return knowledgeService.getSubArticleTree(tenantId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permissionService.requireSchreiben();
        String tenantId = securityHelper.getCurrentTenantId();
        knowledgeService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
