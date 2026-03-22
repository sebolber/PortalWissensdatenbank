package de.wissensdatenbank.retrieval;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.Seg4Recommendation;
import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.repository.KnowledgeItemRepository;
import de.wissensdatenbank.repository.Seg4RecommendationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sucht passende Wissensobjekte und liefert gerankte Kandidaten.
 * Kombiniert PostgreSQL-Volltext mit dem KnowledgeMatchEngine-Scoring.
 */
@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final KnowledgeItemRepository repository;
    private final Seg4RecommendationRepository seg4Repository;
    private final KnowledgeMatchEngine matchEngine;
    private final KnowledgeRankingService rankingService;

    public KnowledgeSearchService(KnowledgeItemRepository repository,
                                   Seg4RecommendationRepository seg4Repository,
                                   KnowledgeMatchEngine matchEngine,
                                   KnowledgeRankingService rankingService) {
        this.repository = repository;
        this.seg4Repository = seg4Repository;
        this.matchEngine = matchEngine;
        this.rankingService = rankingService;
    }

    /**
     * Sucht und rankt Wissensobjekte für eine strukturierte Anfrage.
     */
    public List<KnowledgeCandidate> search(SearchQuery query) {
        // 1. Volltext-Vorfilterung via PostgreSQL (KnowledgeItems)
        List<KnowledgeItem> rawResults = fetchCandidates(query);
        log.debug("Volltext-Suche: {} Rohkandidaten fuer Mandant {}", rawResults.size(), query.tenantId());

        // 2. Direkte SEG4-Suche: passende Empfehlungen finden
        List<Seg4Recommendation> matchedSeg4 = fetchSeg4Candidates(query);
        log.info("SEG4-Suche: {} passende Empfehlungen gefunden", matchedSeg4.size());

        // 3. SEG4-Ergebnisse den KnowledgeItems zuordnen
        Map<Long, List<Seg4Recommendation>> seg4ByItemId = new LinkedHashMap<>();
        for (Seg4Recommendation rec : matchedSeg4) {
            KnowledgeItem parent = rec.getKnowledgeItem();
            seg4ByItemId.computeIfAbsent(parent.getId(), k -> new ArrayList<>()).add(rec);
            // Parent-Item auch in die Ergebnisliste aufnehmen falls noch nicht vorhanden
            if (rawResults.stream().noneMatch(r -> r.getId().equals(parent.getId()))) {
                rawResults.add(parent);
            }
        }

        // 4. LEX_SPECIALIS filtern + Kandidaten bauen
        List<KnowledgeCandidate> candidates = new ArrayList<>();
        for (KnowledgeItem item : rawResults) {
            if (item.getBindingLevel() == BindingLevel.LEX_SPECIALIS) {
                double score = matchEngine.computeScore(query, item);
                if (score < 5.0 && !seg4ByItemId.containsKey(item.getId())) {
                    continue;
                }
            }

            KnowledgeCandidate candidate = new KnowledgeCandidate(item);
            candidate.setScore(matchEngine.computeScore(query, item));
            candidate.setMatchReason(matchEngine.computeMatchReason(query, item));

            // Nur die passenden SEG4-Empfehlungen zuweisen (nicht alle 915)
            List<Seg4Recommendation> matched = seg4ByItemId.get(item.getId());
            if (matched != null) {
                candidate.setMatchedRecommendations(matched);
                candidate.addScore(matched.size() * 2.0);
                candidate.setMatchReason(candidate.getMatchReason()
                        + " SEG4-Treffer: " + matched.size() + " Empfehlungen;");
            }

            candidates.add(candidate);
        }

        // 5. Ranking
        List<KnowledgeCandidate> ranked = rankingService.rank(candidates, query);
        log.debug("Ranking abgeschlossen: {} Kandidaten", ranked.size());
        return ranked;
    }

    private List<KnowledgeItem> fetchCandidates(SearchQuery query) {
        List<KnowledgeItem> results = new ArrayList<>();

        if (query.freitextQuery() != null && !query.freitextQuery().isBlank()) {
            results.addAll(repository.fullTextSearch(query.tenantId(), query.freitextQuery()));
        }

        for (String diagnose : query.diagnosen()) {
            List<KnowledgeItem> found = repository.fullTextSearch(query.tenantId(), diagnose);
            for (KnowledgeItem item : found) {
                if (results.stream().noneMatch(r -> r.getId().equals(item.getId()))) {
                    results.add(item);
                }
            }
        }

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

    private List<Seg4Recommendation> fetchSeg4Candidates(SearchQuery query) {
        List<Seg4Recommendation> results = new ArrayList<>();

        if (query.freitextQuery() != null && !query.freitextQuery().isBlank()) {
            results.addAll(seg4Repository.fullTextSearch(query.tenantId(), query.freitextQuery()));
        }

        for (String diagnose : query.diagnosen()) {
            List<Seg4Recommendation> found = seg4Repository.fullTextSearch(query.tenantId(), diagnose);
            for (Seg4Recommendation rec : found) {
                if (results.stream().noneMatch(r -> r.getId().equals(rec.getId()))) {
                    results.add(rec);
                }
            }
        }

        for (String massnahme : query.massnahmen()) {
            List<Seg4Recommendation> found = seg4Repository.fullTextSearch(query.tenantId(), massnahme);
            for (Seg4Recommendation rec : found) {
                if (results.stream().noneMatch(r -> r.getId().equals(rec.getId()))) {
                    results.add(rec);
                }
            }
        }

        return results;
    }
}
