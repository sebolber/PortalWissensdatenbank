package de.wissensdatenbank.controller;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.SuggestionRequest;
import de.wissensdatenbank.dto.SuggestionResponse;
import de.wissensdatenbank.service.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST-API für KI-gestützte Kodierempfehlungen.
 */
@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    private final SuggestionService suggestionService;
    private final SecurityHelper securityHelper;

    public SuggestionController(SuggestionService suggestionService,
                                 SecurityHelper securityHelper) {
        this.suggestionService = suggestionService;
        this.securityHelper = securityHelper;
    }

    /**
     * POST /api/suggestions
     * Generiert eine KI-Kodierempfehlung basierend auf Behandlungsdokument.
     */
    @PostMapping
    public ResponseEntity<SuggestionResponse> generateSuggestion(
            @RequestBody SuggestionRequest request) {
        String tenantId = securityHelper.getCurrentTenantId();
        String userId = securityHelper.getCurrentUserId();
        String jwtToken = securityHelper.getCurrentToken();

        SuggestionResponse response = suggestionService.generateSuggestion(
                tenantId, userId, jwtToken, request);

        return ResponseEntity.ok(response);
    }
}
