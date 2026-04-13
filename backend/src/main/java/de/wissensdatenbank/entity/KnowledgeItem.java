package de.wissensdatenbank.entity;

import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.enums.KnowledgeType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Zentrales Wissensobjekt – kann ein SEG4-Import, ein Artikel oder eine Leitlinie sein.
 */
@Entity
@Table(name = "wb_knowledge_items", indexes = {
        @Index(name = "idx_ki_tenant", columnList = "tenant_id"),
        @Index(name = "idx_ki_tenant_type", columnList = "tenant_id, knowledge_type"),
        @Index(name = "idx_ki_tenant_binding", columnList = "tenant_id, binding_level")
})
public class KnowledgeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "knowledge_type", nullable = false, length = 30)
    private KnowledgeType knowledgeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "binding_level", nullable = false, length = 30)
    private BindingLevel bindingLevel;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "source_reference", length = 500)
    private String sourceReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_version_id")
    private ProductVersion productVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @ManyToMany
    @JoinTable(
            name = "wb_knowledge_item_tags",
            joinColumns = @JoinColumn(name = "knowledge_item_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "knowledgeItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<KnowledgeSubArticle> subArticles = new ArrayList<>();

    @OneToMany(mappedBy = "knowledgeItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Seg4Recommendation> seg4Recommendations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public KnowledgeType getKnowledgeType() { return knowledgeType; }
    public void setKnowledgeType(KnowledgeType knowledgeType) { this.knowledgeType = knowledgeType; }

    public BindingLevel getBindingLevel() { return bindingLevel; }
    public void setBindingLevel(BindingLevel bindingLevel) { this.bindingLevel = bindingLevel; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }

    public LocalDateTime getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDateTime validUntil) { this.validUntil = validUntil; }

    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public ProductVersion getProductVersion() { return productVersion; }
    public void setProductVersion(ProductVersion productVersion) { this.productVersion = productVersion; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Set<Tag> getTags() { return tags; }
    public void setTags(Set<Tag> tags) { this.tags = tags; }

    public List<KnowledgeSubArticle> getSubArticles() { return subArticles; }
    public void setSubArticles(List<KnowledgeSubArticle> subArticles) { this.subArticles = subArticles; }

    public List<Seg4Recommendation> getSeg4Recommendations() { return seg4Recommendations; }
    public void setSeg4Recommendations(List<Seg4Recommendation> seg4Recommendations) { this.seg4Recommendations = seg4Recommendations; }
}
