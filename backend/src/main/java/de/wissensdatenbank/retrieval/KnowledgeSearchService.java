package de.wissensdatenbank.retrieval;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.repository.KnowledgeItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Sucht passende Wissensobjekte und liefert gerankte Kandidaten.
 * Kombiniert PostgreSQL-Volltext mit dem KnowledgeMatchEngine-Scoring.
 */
@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final KnowledgeItemRepository repository;
    private final KnowledgeMatchEngine matchEngine;
    private final KnowledgeRankingService rankingService;

    public KnowledgeSearchService(KnowledgeItemRepository repository,
                                   KnowledgeMatchEngine matchEngine,
                                   KnowledgeRankingService rankingService) {
        this.repository = repository;
        this.matchEngine = matchEngine;
        this.rankingService = rankingService;
    }

    /**
     * Sucht und rankt Wissensobjekte für eine strukturierte Anfrage.
     *
     * @param query Suchanfrage mit Diagnosen, Maßnahmen, Keywords
     * @return Top-5 gerankte Kandidaten
     */
    public List<KnowledgeCandidate> search(SearchQuery query) {
        // 1. Volltext-Vorfilterung via PostgreSQL
        List<KnowledgeItem> rawResults = fetchCandidates(query);
        log.debug("Volltext-Suche: {} Rohkandidaten fuer Mandant {}", rawResults.size(), query.tenantId());

        // 2. LEX_SPECIALIS filtern: nur bei exaktem Treffer einbeziehen
        List<KnowledgeCandidate> candidates = new ArrayList<>();
        for (KnowledgeItem item : rawResults) {
            if (item.getBindingLevel() == BindingLevel.LEX_SPECIALIS) {
                double score = matchEngine.computeScore(query, item);
                if (score < 5.0) {
                    continue; // LEX_SPECIALIS nur bei starkem Match
                }
            }

            KnowledgeCandidate candidate = new KnowledgeCandidate(item);
            candidate.setScore(matchEngine.computeScore(query, item));
            candidate.setMatchReason(matchEngine.computeMatchReason(query, item));
            candidates.add(candidate);
        }

        // 3. Ranking
        List<KnowledgeCandidate> ranked = rankingService.rank(candidates, query);
        log.debug("Ranking abgeschlossen: {} Kandidaten", ranked.size());
        return ranked;
    }

    private List<KnowledgeItem> fetchCandidates(SearchQuery query) {
        List<KnowledgeItem> results = new ArrayList<>();

        // Freitext-Suche
        if (query.freitextQuery() != null && !query.freitextQuery().isBlank()) {
            results.addAll(repository.fullTextSearch(query.tenantId(), query.freitextQuery()));
        }

        // Zusätzliche Suche mit Diagnosen
        for (String diagnose : query.diagnosen()) {
            List<KnowledgeItem> found = repository.fullTextSearch(query.tenantId(), diagnose);
            for (KnowledgeItem item : found) {
                if (results.stream().noneMatch(r -> r.getId().equals(item.getId()))) {
                    results.add(item);
                }
            }
        }

        // Zusätzliche Suche mit Maßnahmen
        for (String massnahme : query.massnahmen()) {
            List<KnowledgeItem> found = repository.fullTextSearch(query.tenantId(), massnahme);
            for (KnowledgeItem item : found) {
                if (results.stream().noneMatch(r -> r.getId().equals(item.getId()))) {
                    results.add(item);
                }
            }
        }

        return results;
    }
}
