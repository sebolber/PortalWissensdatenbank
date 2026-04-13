package de.wissensdatenbank.entity;

import jakarta.persistence.*;

/**
 * Querverweis zwischen Wissensartikeln bzw. deren Abschnitten
 * (z.B. "siehe Abschnitt 3.3.16").
 */
@Entity
@Table(name = "wb_knowledge_cross_references", indexes = {
        @Index(name = "idx_kcr_source", columnList = "source_item_id"),
        @Index(name = "idx_kcr_target", columnList = "target_item_id"),
        @Index(name = "idx_kcr_source_sub", columnList = "source_sub_article_id")
})
public class KnowledgeCrossReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_item_id", nullable = false)
    private KnowledgeItem sourceItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_sub_article_id")
    private KnowledgeSubArticle sourceSubArticle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_item_id", nullable = false)
    private KnowledgeItem targetItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_sub_article_id")
    private KnowledgeSubArticle targetSubArticle;

    @Column(name = "reference_text", length = 500)
    private String referenceText;

    @Column(name = "reference_type", nullable = false, length = 30)
    private String referenceType = "SEE_ALSO";

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KnowledgeItem getSourceItem() { return sourceItem; }
    public void setSourceItem(KnowledgeItem sourceItem) { this.sourceItem = sourceItem; }

    public KnowledgeSubArticle getSourceSubArticle() { return sourceSubArticle; }
    public void setSourceSubArticle(KnowledgeSubArticle sourceSubArticle) { this.sourceSubArticle = sourceSubArticle; }

    public KnowledgeItem getTargetItem() { return targetItem; }
    public void setTargetItem(KnowledgeItem targetItem) { this.targetItem = targetItem; }

    public KnowledgeSubArticle getTargetSubArticle() { return targetSubArticle; }
    public void setTargetSubArticle(KnowledgeSubArticle targetSubArticle) { this.targetSubArticle = targetSubArticle; }

    public String getReferenceText() { return referenceText; }
    public void setReferenceText(String referenceText) { this.referenceText = referenceText; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
}
