package de.wissensdatenbank.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Revisionssicherer Audit-Trail für KI-Kodierempfehlungen.
 * Speichert Input, Prompt, Antwort und Metadaten.
 */
@Entity
@Table(name = "wb_suggestion_audit_log", indexes = {
        @Index(name = "idx_sal_tenant", columnList = "tenant_id"),
        @Index(name = "idx_sal_tenant_created", columnList = "tenant_id, created_at")
})
public class SuggestionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "input_document_text", columnDefinition = "TEXT")
    private String inputDocumentText;

    @Column(name = "used_knowledge_item_ids", length = 500)
    private String usedKnowledgeItemIds;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt", columnDefinition = "TEXT")
    private String userPrompt;

    @Column(name = "llm_response", columnDefinition = "TEXT")
    private String llmResponse;

    @Column(name = "llm_model", length = 255)
    private String llmModel;

    @Column(name = "llm_config_id", length = 255)
    private String llmConfigId;

    @Column(name = "token_count")
    private int tokenCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getInputDocumentText() { return inputDocumentText; }
    public void setInputDocumentText(String inputDocumentText) { this.inputDocumentText = inputDocumentText; }

    public String getUsedKnowledgeItemIds() { return usedKnowledgeItemIds; }
    public void setUsedKnowledgeItemIds(String usedKnowledgeItemIds) { this.usedKnowledgeItemIds = usedKnowledgeItemIds; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }

    public String getLlmResponse() { return llmResponse; }
    public void setLlmResponse(String llmResponse) { this.llmResponse = llmResponse; }

    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }

    public String getLlmConfigId() { return llmConfigId; }
    public void setLlmConfigId(String llmConfigId) { this.llmConfigId = llmConfigId; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
