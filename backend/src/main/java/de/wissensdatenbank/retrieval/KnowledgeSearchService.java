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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sucht passende Wissensobjekte und liefert gerankte Kandidaten.
 * Kombiniert PostgreSQL-Volltext mit dem KnowledgeMatchEngine-Scoring.
 */
@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    /** Deutsche Stoppwoerter, die fuer die Suche irrelevant sind. */
    private static final Set<String> STOP_WORDS = Set.of(
            "der", "die", "das", "den", "dem", "des", "ein", "eine", "einer", "einem", "einen",
            "und", "oder", "aber", "auch", "als", "auf", "aus", "bei", "bis", "nach", "von",
            "vor", "mit", "fuer", "ist", "sind", "war", "wird", "wurde", "werden", "hat", "haben",
            "nicht", "noch", "nur", "dass", "sich", "sie", "ich", "wir", "man", "kann", "soll",
            "dann", "wenn", "ueber", "unter", "zum", "zur", "vom", "durch", "ohne", "gegen",
            "bereits", "sehr", "mehr", "keine", "kein", "alle", "jede", "andere", "dieser",
            "einem", "dieses", "diese", "welche", "welcher", "welches", "sowie", "bzw",
            "wurde", "worden", "werden", "hatte", "hatten", "konnte", "konnten", "sollte",
            "wie", "was", "wer", "wann", "warum", "hier", "dort", "dazu"
    );

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
     * Sucht und rankt Wissensobjekte fuer eine strukturierte Anfrage.
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

        // Diagnosen und Massnahmen als einzelne Suchterme (kurz, spezifisch)
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

        // Freitext: Schluesselwoerter extrahieren und einzeln suchen (statt gesamten Text)
        if (query.freitextQuery() != null && !query.freitextQuery().isBlank()) {
            List<String> keyTerms = extractKeyTerms(query.freitextQuery());
            // Nur die ersten 5 Schluesselwoerter als Suche verwenden
            for (String term : keyTerms.stream().limit(5).toList()) {
                try {
                    List<KnowledgeItem> found = repository.fullTextSearch(query.tenantId(), term);
                    for (KnowledgeItem item : found) {
                        if (results.stream().noneMatch(r -> r.getId().equals(item.getId()))) {
                            results.add(item);
                        }
                    }
                } catch (Exception e) {
                    log.debug("Suche mit Term '{}' fehlgeschlagen: {}", term, e.getMessage());
                }
            }
        }

        return results;
    }

    /**
     * Sucht passende SEG4-Empfehlungen mit OR-verknuepften Suchbegriffen.
     * Extrahiert medizinische Schluesselwoerter aus dem Dokumenttext statt
     * den gesamten Text als AND-Query zu verwenden.
     */
    private List<Seg4Recommendation> fetchSeg4Candidates(SearchQuery query) {
        List<Seg4Recommendation> results = new ArrayList<>();

        // Alle Suchterme sammeln: Diagnosen + Massnahmen + Schluesselwoerter aus Freitext
        List<String> searchTerms = new ArrayList<>();
        searchTerms.addAll(query.diagnosen());
        searchTerms.addAll(query.massnahmen());

        // Aus dem Dokumenttext medizinische Schluesselwoerter extrahieren
        if (query.freitextQuery() != null && !query.freitextQuery().isBlank()) {
            searchTerms.addAll(extractKeyTerms(query.freitextQuery()));
        }

        if (searchTerms.isEmpty()) {
            return results;
        }

        // OR-verknuepfte tsquery bauen: "term1 | term2 | term3"
        String tsQuery = buildOrTsQuery(searchTerms);
        if (tsQuery == null || tsQuery.isBlank()) {
            return results;
        }

        log.info("SEG4-Suche mit tsquery: {}", tsQuery.length() > 200 ? tsQuery.substring(0, 200) + "..." : tsQuery);

        try {
            results.addAll(seg4Repository.fullTextSearch(query.tenantId(), tsQuery));
        } catch (Exception e) {
            log.warn("SEG4-Volltextsuche fehlgeschlagen: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Extrahiert die wichtigsten medizinischen Begriffe aus einem Dokumenttext.
     * Filtert Stoppwoerter, kurze Woerter und behaelt max. 30 Terme.
     */
    private List<String> extractKeyTerms(String text) {
        return Arrays.stream(text.split("[\\s,;.:\\-()\\[\\]/\"'!?]+"))
                .map(String::toLowerCase)
                .map(s -> s.replaceAll("[^a-zäöüß0-9.]", ""))
                .filter(s -> s.length() >= 3)
                .filter(s -> !STOP_WORDS.contains(s))
                .distinct()
                .limit(30)
                .collect(Collectors.toList());
    }

    /**
     * Baut eine OR-verknuepfte tsquery aus einer Liste von Suchbegriffen.
     * Ergebnis: "term1 | term2 | term3" (fuer to_tsquery('german', ...)).
     */
    private String buildOrTsQuery(List<String> terms) {
        List<String> sanitized = terms.stream()
                .map(t -> t.replaceAll("[^a-zA-ZäöüÄÖÜß0-9.]", " ").trim())
                .filter(t -> !t.isBlank())
                .flatMap(t -> Arrays.stream(t.split("\\s+")))
                .filter(t -> t.length() >= 2)
                .filter(t -> !STOP_WORDS.contains(t.toLowerCase()))
                .distinct()
                .limit(40)
                .map(t -> "'" + t.replace("'", "") + "'")
                .collect(Collectors.toList());

        if (sanitized.isEmpty()) {
            return null;
        }

        return String.join(" | ", sanitized);
    }
}
