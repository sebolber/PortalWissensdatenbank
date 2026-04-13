package de.wissensdatenbank.controller;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.SuggestionRequest;
import de.wissensdatenbank.dto.SuggestionResponse;
import de.wissensdatenbank.llm.LlmModelResolutionService;
import de.wissensdatenbank.service.PermissionService;
import de.wissensdatenbank.service.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-API für KI-gestützte Kodierempfehlungen.
 */
@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;
    private final LlmModelResolutionService llmModelService;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public SuggestionController(SuggestionService suggestionService,
                                 LlmModelResolutionService llmModelService,
                                 SecurityHelper securityHelper,
                                 PermissionService permissionService) {
        this.suggestionService = suggestionService;
        this.llmModelService = llmModelService;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    /**
     * POST /api/suggestions
     * Generiert eine KI-Kodierempfehlung basierend auf Behandlungsdokument.
     */
    @PostMapping
    public ResponseEntity<SuggestionResponse> generateSuggestion(
            @RequestBody SuggestionRequest request) {
        permissionService.requireSchreiben();
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        String jwtToken = securityHelper.getCurrentToken();

        SuggestionResponse response = suggestionService.generateSuggestion(
                tenantId, userId, jwtToken, request);

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/suggestions/llm-models
     * Listet alle verfuegbaren LLM-Konfigurationen des Mandanten.
     */
    @GetMapping("/llm-models")
    public ResponseEntity<List<Map<String, Object>>> listLlmModels() {
        permissionService.requireLesen();
        String tenantId = securityHelper.getCurrentTenantId();
        String jwtToken = securityHelper.getCurrentToken();
        return ResponseEntity.ok(llmModelService.listLlmModels(tenantId, jwtToken));
    }
}
