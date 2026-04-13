package de.wissensdatenbank.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Unterartikel eines KnowledgeItems – z.B. einzelne Abschnitte eines Leitfadens.
 * Unterstützt hierarchische Verschachtelung (self-referencing parent) für
 * Handbuch-Strukturen mit 5+ Ebenen (z.B. Abschnitt 3.3.14.4.12).
 */
@Entity
@Table(name = "wb_knowledge_sub_articles", indexes = {
        @Index(name = "idx_ksa_ki", columnList = "knowledge_item_id"),
        @Index(name = "idx_ksa_parent", columnList = "parent_id"),
        @Index(name = "idx_ksa_section", columnList = "section_number")
})
public class KnowledgeSubArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private KnowledgeSubArticle parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<KnowledgeSubArticle> children = new ArrayList<>();

    @Column(nullable = false, length = 500)
    private String heading;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    /** Strukturierte Abschnittsnummer, z.B. "3.3.14.4.12" */
    @Column(name = "section_number", length = 50)
    private String sectionNumber;

    /** Verschachtelungstiefe (0 = direktes Kind des KnowledgeItem) */
    @Column(nullable = false)
    private int depth = 0;

    /** Materialized Path für effiziente Unterbaum-Abfragen, z.B. "/5/23/117/" */
    @Column(columnDefinition = "TEXT")
    private String path;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KnowledgeItem getKnowledgeItem() { return knowledgeItem; }
    public void setKnowledgeItem(KnowledgeItem knowledgeItem) { this.knowledgeItem = knowledgeItem; }

    public KnowledgeSubArticle getParent() { return parent; }
    public void setParent(KnowledgeSubArticle parent) { this.parent = parent; }

    public List<KnowledgeSubArticle> getChildren() { return children; }
    public void setChildren(List<KnowledgeSubArticle> children) { this.children = children; }

    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public String getSectionNumber() { return sectionNumber; }
    public void setSectionNumber(String sectionNumber) { this.sectionNumber = sectionNumber; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
