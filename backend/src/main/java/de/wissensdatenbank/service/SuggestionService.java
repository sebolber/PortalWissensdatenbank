package de.wissensdatenbank.service;

import de.wissensdatenbank.audit.AuditService;
import de.wissensdatenbank.dto.SuggestionRequest;
import de.wissensdatenbank.dto.SuggestionResponse;
import de.wissensdatenbank.entity.SuggestionAuditLog;
import de.wissensdatenbank.llm.*;
import de.wissensdatenbank.retrieval.KnowledgeCandidate;
import de.wissensdatenbank.retrieval.KnowledgeSearchService;
import de.wissensdatenbank.retrieval.SearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestriert die KI-gestützte Kodierempfehlung:
 * Dokument analysieren → Wissen suchen → Prompt bauen → LLM aufrufen → Audit loggen.
 */
@Service
@Transactional(readOnly = true)
public class SuggestionService {

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);

    private final KnowledgeSearchService searchService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final AuditService auditService;

    public SuggestionService(KnowledgeSearchService searchService,
                              PromptBuilder promptBuilder,
                              LlmClient llmClient,
                              AuditService auditService) {
        this.searchService = searchService;
        this.promptBuilder = promptBuilder;
        this.llmClient = llmClient;
        this.auditService = auditService;
    }

    /**
     * Generiert eine strukturierte Kodierempfehlung.
     */
    @Transactional
    public SuggestionResponse generateSuggestion(String tenantId, String userId,
                                                   String jwtToken, SuggestionRequest request) {
        // 1. Retrieval: passende Wissensobjekte suchen
        SearchQuery searchQuery = new SearchQuery(
                request.dokumentText(),
                request.diagnosen(),
                request.massnahmen(),
                List.of(),
                tenantId
        );
        List<KnowledgeCandidate> candidates = searchService.search(searchQuery);
        log.info("Retrieval: {} Kandidaten fuer Mandant {}", candidates.size(), tenantId);

        // 2. Prompt bauen (nur strukturierte Daten)
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(
                request.diagnosen(), request.massnahmen(),
                request.dokumentText(), candidates
        );

        // 3. LLM aufrufen
        LlmRequest llmRequest = new LlmRequest(tenantId, jwtToken, systemPrompt, userPrompt);
        LlmResponse llmResponse = llmClient.chat(llmRequest);

        // 4. Audit loggen
        SuggestionAuditLog auditLog = auditService.log(
                tenantId, userId, request.dokumentText(),
                systemPrompt, userPrompt, llmResponse, candidates
        );

        // 5. Response bauen
        List<SuggestionResponse.UsedSource> quellen = candidates.stream()
                .map(c -> new SuggestionResponse.UsedSource(
                        c.getItem().getId(),
                        c.getItem().getTitle(),
                        c.getItem().getBindingLevel().name(),
                        c.getMatchReason()
                ))
                .toList();

        return new SuggestionResponse(
                llmResponse.content(),
                llmResponse.model(),
                llmResponse.tokenCount(),
                quellen,
                auditLog.getId()
        );
    }
}
