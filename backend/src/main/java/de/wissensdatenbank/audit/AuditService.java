package de.wissensdatenbank.audit;

import de.wissensdatenbank.entity.SuggestionAuditLog;
import de.wissensdatenbank.llm.LlmResponse;
import de.wissensdatenbank.repository.SuggestionAuditLogRepository;
import de.wissensdatenbank.retrieval.KnowledgeCandidate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Revisionssicheres Audit-Logging für KI-Kodierempfehlungen.
 */
@Service
public class AuditService {

    private final SuggestionAuditLogRepository repository;

    public AuditService(SuggestionAuditLogRepository repository) {
        this.repository = repository;
    }

    /**
     * Protokolliert eine vollständige KI-Empfehlung.
     */
    public SuggestionAuditLog log(String tenantId, String userId,
                                   String inputText, String systemPrompt,
                                   String userPrompt, LlmResponse llmResponse,
                                   List<KnowledgeCandidate> usedCandidates) {
        SuggestionAuditLog entry = new SuggestionAuditLog();
        entry.setTenantId(tenantId);
        entry.setUserId(userId);
        entry.setInputDocumentText(inputText);
        entry.setSystemPrompt(systemPrompt);
        entry.setUserPrompt(userPrompt);
        entry.setLlmResponse(llmResponse.content());
        entry.setLlmModel(llmResponse.model());
        entry.setLlmConfigId(llmResponse.configId());
        entry.setTokenCount(llmResponse.tokenCount());
        entry.setUsedKnowledgeItemIds(
                usedCandidates.stream()
                        .map(c -> String.valueOf(c.getItem().getId()))
                        .collect(Collectors.joining(","))
        );
        return repository.save(entry);
    }
}
