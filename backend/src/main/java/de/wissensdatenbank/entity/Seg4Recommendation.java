package de.wissensdatenbank.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * Eine einzelne geparsete SEG4-Kodierempfehlung innerhalb eines KnowledgeItems.
 * Enthält die strukturierten Felder aus dem SEG4-PDF.
 */
@Entity
@Table(name = "wb_seg4_recommendations", indexes = {
        @Index(name = "idx_seg4_ki", columnList = "knowledge_item_id"),
        @Index(name = "idx_seg4_number", columnList = "recommendation_number")
})
public class Seg4Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_item_id", nullable = false)
    private KnowledgeItem knowledgeItem;

    @Column(name = "recommendation_number", length = 50)
    private String recommendationNumber;

    @Column(length = 1000)
    private String schlagworte;

    @Column(name = "erstellt_am")
    private LocalDate erstelltAm;

    @Column(name = "aktualisiert_am")
    private LocalDate aktualisiertAm;

    @Column(name = "problem_erlaeuterung", columnDefinition = "TEXT")
    private String problemErlaeuterung;

    @Column(columnDefinition = "TEXT")
    private String empfehlung;

    @Column(columnDefinition = "TEXT")
    private String entscheidung;

    @Column(columnDefinition = "TEXT")
    private String zusatzhinweis;

    @Column(name = "is_arbitration", nullable = false)
    private boolean arbitration = false;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public KnowledgeItem getKnowledgeItem() { return knowledgeItem; }
    public void setKnowledgeItem(KnowledgeItem knowledgeItem) { this.knowledgeItem = knowledgeItem; }

    public String getRecommendationNumber() { return recommendationNumber; }
    public void setRecommendationNumber(String recommendationNumber) { this.recommendationNumber = recommendationNumber; }

    public String getSchlagworte() { return schlagworte; }
    public void setSchlagworte(String schlagworte) { this.schlagworte = schlagworte; }

    public LocalDate getErstelltAm() { return erstelltAm; }
    public void setErstelltAm(LocalDate erstelltAm) { this.erstelltAm = erstelltAm; }

    public LocalDate getAktualisiertAm() { return aktualisiertAm; }
    public void setAktualisiertAm(LocalDate aktualisiertAm) { this.aktualisiertAm = aktualisiertAm; }

    public String getProblemErlaeuterung() { return problemErlaeuterung; }
    public void setProblemErlaeuterung(String problemErlaeuterung) { this.problemErlaeuterung = problemErlaeuterung; }

    public String getEmpfehlung() { return empfehlung; }
    public void setEmpfehlung(String empfehlung) { this.empfehlung = empfehlung; }

    public String getEntscheidung() { return entscheidung; }
    public void setEntscheidung(String entscheidung) { this.entscheidung = entscheidung; }

    public String getZusatzhinweis() { return zusatzhinweis; }
    public void setZusatzhinweis(String zusatzhinweis) { this.zusatzhinweis = zusatzhinweis; }

    public boolean isArbitration() { return arbitration; }
    public void setArbitration(boolean arbitration) { this.arbitration = arbitration; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }
}
