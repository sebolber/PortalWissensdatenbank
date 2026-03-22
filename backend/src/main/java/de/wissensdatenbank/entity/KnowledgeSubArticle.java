package de.wissensdatenbank.entity;

import jakarta.persistence.*;

/**
 * Unterartikel eines KnowledgeItems – z.B. einzelne Abschnitte eines Leitfadens.
 */
@Entity
@Table(name = "wb_knowledge_sub_articles", indexes = {
        @Index(name = "idx_ksa_ki", columnList = "knowledge_item_id")
})
public class KnowledgeSubArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    @Column(nullable = false, length = 500)
    private String heading;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KnowledgeItem getKnowledgeItem() { return knowledgeItem; }
    public void setKnowledgeItem(KnowledgeItem knowledgeItem) { this.knowledgeItem = knowledgeItem; }

    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
