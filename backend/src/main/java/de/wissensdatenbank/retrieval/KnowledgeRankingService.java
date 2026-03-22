package de.wissensdatenbank.retrieval;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.enums.BindingLevel;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Rankt eine Liste von KnowledgeCandidates nach:
 * 1. Match-Score (aus KnowledgeMatchEngine)
 * 2. BindingLevel-Gewichtung
 * 3. Aktualität
 * 4. LEX_SPECIALIS nur bei exact match
 */
@Service
public class KnowledgeRankingService {

    private static final int TOP_N = 5;

    /**
     * Rankt Kandidaten und gibt die Top-N zurück.
     */
    public List<KnowledgeCandidate> rank(List<KnowledgeCandidate> candidates, SearchQuery query) {
        for (KnowledgeCandidate candidate : candidates) {
            double adjusted = candidate.getScore();
            adjusted += bindingLevelBonus(candidate.getItem(), query);
            adjusted += recencyBonus(candidate.getItem());
            candidate.setScore(adjusted);
        }

        return candidates.stream()
                .filter(c -> c.getScore() > 0.0)
                .sorted(Comparator.comparingDouble(KnowledgeCandidate::getScore).reversed())
                .limit(TOP_N)
                .toList();
    }

    /**
     * BindingLevel-Gewichtung:
     * VERBINDLICH = +2.0
     * EMPFEHLUNG = +1.0
     * LEX_SPECIALIS = +3.0 (nur bei hohem Base-Score, sonst 0)
     * INFORMATIV = +0.5
     */
    double bindingLevelBonus(KnowledgeItem item, SearchQuery query) {
        BindingLevel level = item.getBindingLevel();
        if (level == null) return 0.0;

        return switch (level) {
            case VERBINDLICH -> 2.0;
            case EMPFEHLUNG -> 1.0;
            case LEX_SPECIALIS -> 3.0; // gefiltert durch KnowledgeSearchService
            case INFORMATIV -> 0.5;
        };
    }

    /**
     * Aktualitätsbonus: je neuer, desto höher (max +1.0).
     */
    double recencyBonus(KnowledgeItem item) {
        LocalDateTime updated = item.getUpdatedAt();
        if (updated == null) return 0.0;

        long daysAgo = java.time.Duration.between(updated, LocalDateTime.now()).toDays();
        if (daysAgo <= 90) return 1.0;
        if (daysAgo <= 365) return 0.5;
        return 0.2;
    }
}
