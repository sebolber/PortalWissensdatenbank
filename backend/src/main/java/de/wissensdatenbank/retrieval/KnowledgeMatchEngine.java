package de.wissensdatenbank.retrieval;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.entity.Seg4Recommendation;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Berechnet Ähnlichkeits-Scores zwischen einer SearchQuery und einem KnowledgeItem.
 * Reine Score-Berechnung – keine DB-Zugriffe.
 */
@Component
public class KnowledgeMatchEngine {

    /**
     * Berechnet den Gesamt-Score eines KnowledgeItems für eine Suchanfrage.
     */
    public double computeScore(SearchQuery query, KnowledgeItem item) {
        double score = 0.0;

        score += keywordMatchScore(query, item) * 3.0;
        score += diagnoseMatchScore(query, item) * 2.5;
        score += massnahmenMatchScore(query, item) * 2.0;
        score += seg4ContentScore(query, item) * 2.0;

        return score;
    }

    /**
     * Erzeugt eine menschenlesbare Match-Begründung.
     */
    public String computeMatchReason(SearchQuery query, KnowledgeItem item) {
        StringBuilder sb = new StringBuilder();
        if (keywordMatchScore(query, item) > 0) sb.append("Keyword-Treffer; ");
        if (diagnoseMatchScore(query, item) > 0) sb.append("Diagnose-Treffer; ");
        if (massnahmenMatchScore(query, item) > 0) sb.append("Massnahmen-Treffer; ");
        if (seg4ContentScore(query, item) > 0) sb.append("SEG4-Inhalt-Treffer; ");
        return sb.isEmpty() ? "Volltextsuche" : sb.toString().trim();
    }

    double keywordMatchScore(SearchQuery query, KnowledgeItem item) {
        Set<String> itemKeywords = tokenize(item.getKeywords());
        if (itemKeywords.isEmpty()) return 0.0;

        Set<String> queryTerms = tokenize(String.join(" ", query.keywords()));
        if (queryTerms.isEmpty() && query.freitextQuery() != null) {
            queryTerms = tokenize(query.freitextQuery());
        }

        long matches = queryTerms.stream()
                .filter(qt -> itemKeywords.stream().anyMatch(ik -> ik.contains(qt) || qt.contains(ik)))
                .count();

        return queryTerms.isEmpty() ? 0.0 : (double) matches / queryTerms.size();
    }

    double diagnoseMatchScore(SearchQuery query, KnowledgeItem item) {
        if (query.diagnosen().isEmpty()) return 0.0;
        String searchable = buildSearchableText(item);
        long matches = query.diagnosen().stream()
                .filter(d -> containsIgnoreCase(searchable, d))
                .count();
        return (double) matches / query.diagnosen().size();
    }

    double massnahmenMatchScore(SearchQuery query, KnowledgeItem item) {
        if (query.massnahmen().isEmpty()) return 0.0;
        String searchable = buildSearchableText(item);
        long matches = query.massnahmen().stream()
                .filter(m -> containsIgnoreCase(searchable, m))
                .count();
        return (double) matches / query.massnahmen().size();
    }

    double seg4ContentScore(SearchQuery query, KnowledgeItem item) {
        List<Seg4Recommendation> recs = item.getSeg4Recommendations();
        if (recs == null || recs.isEmpty()) return 0.0;

        Set<String> queryTerms = tokenize(query.freitextQuery());
        if (queryTerms.isEmpty()) return 0.0;

        double maxScore = 0.0;
        for (Seg4Recommendation rec : recs) {
            String recText = joinNullable(rec.getSchlagworte(), rec.getProblemErlaeuterung(),
                    rec.getEmpfehlung(), rec.getEntscheidung());
            Set<String> recTokens = tokenize(recText);

            long matches = queryTerms.stream()
                    .filter(qt -> recTokens.stream().anyMatch(rt -> rt.contains(qt) || qt.contains(rt)))
                    .count();

            double recScore = (double) matches / queryTerms.size();
            maxScore = Math.max(maxScore, recScore);
        }
        return maxScore;
    }

    private String buildSearchableText(KnowledgeItem item) {
        return joinNullable(item.getTitle(), item.getSummary(), item.getKeywords());
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) return Set.of();
        return Arrays.stream(text.toLowerCase().split("[\\s,;|.:/]+"))
                .map(String::trim)
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && needle != null
                && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private String joinNullable(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null) sb.append(p).append(' ');
        }
        return sb.toString();
    }
}
