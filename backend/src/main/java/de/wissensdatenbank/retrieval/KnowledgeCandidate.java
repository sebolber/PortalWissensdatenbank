package de.wissensdatenbank.retrieval;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.Seg4Recommendation;

import java.util.ArrayList;
import java.util.List;

/**
 * Ein Kandidat aus der Suche mit zugehörigem Relevanz-Score.
 */
public class KnowledgeCandidate {

    private final KnowledgeItem item;
    private double score;
    private String matchReason;
    private List<Seg4Recommendation> matchedRecommendations = new ArrayList<>();

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

    public List<Seg4Recommendation> getMatchedRecommendations() { return matchedRecommendations; }
    public void setMatchedRecommendations(List<Seg4Recommendation> recs) { this.matchedRecommendations = recs; }
}
