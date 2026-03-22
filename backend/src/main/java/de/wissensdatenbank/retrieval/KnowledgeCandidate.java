package de.wissensdatenbank.retrieval;

import de.wissensdatenbank.entity.KnowledgeItem;

/**
 * Ein Kandidat aus der Suche mit zugehörigem Relevanz-Score.
 */
public class KnowledgeCandidate {

    private final KnowledgeItem item;
    private double score;
    private String matchReason;

    public KnowledgeCandidate(KnowledgeItem item) {
        this.item = item;
        this.score = 0.0;
    }

    public KnowledgeItem getItem() { return item; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public void addScore(double delta) { this.score += delta; }

    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }
}
