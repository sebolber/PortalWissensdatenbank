package de.wissensdatenbank.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Dokument-basierte KI-Kodierempfehlung.
 * Speichert das hochgeladene Dokument samt Ergebnis der Kodierempfehlung.
 */
@Entity
@Table(name = "wb_document_suggestions", indexes = {
        @Index(name = "idx_ds_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ds_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_ds_tenant_created", columnList = "tenant_id, created_at")
})
public class DocumentSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_content_type", length = 255)
    private String fileContentType;

    @Column(name = "file_data", columnDefinition = "BYTEA")
    private byte[] fileData;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "PENDING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "empfehlungen", columnDefinition = "TEXT")
    private String empfehlungen;

    @Column(name = "llm_model", length = 255)
    private String llmModel;

    @Column(name = "token_count")
    private int tokenCount;

    @Column(name = "quellen_json", columnDefinition = "TEXT")
    private String quellenJson;

    @Column(name = "audit_log_id")
    private Long auditLogId;

    @Column(name = "model_config_id", length = 255)
    private String modelConfigId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileContentType() { return fileContentType; }
    public void setFileContentType(String fileContentType) { this.fileContentType = fileContentType; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getEmpfehlungen() { return empfehlungen; }
    public void setEmpfehlungen(String empfehlungen) { this.empfehlungen = empfehlungen; }

    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    public String getQuellenJson() { return quellenJson; }
    public void setQuellenJson(String quellenJson) { this.quellenJson = quellenJson; }

    public Long getAuditLogId() { return auditLogId; }
    public void setAuditLogId(Long auditLogId) { this.auditLogId = auditLogId; }

    public String getModelConfigId() { return modelConfigId; }
    public void setModelConfigId(String modelConfigId) { this.modelConfigId = modelConfigId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
