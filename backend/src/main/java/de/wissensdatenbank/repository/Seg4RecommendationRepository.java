package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.Seg4Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Seg4RecommendationRepository extends JpaRepository<Seg4Recommendation, Long> {

    List<Seg4Recommendation> findByKnowledgeItemId(Long knowledgeItemId);

    List<Seg4Recommendation> findByRecommendationNumberContainingIgnoreCase(String number);

    /**
     * Volltextsuche ueber SEG4-Empfehlungsinhalte mit OR-Verknuepfung.
     * Verwendet websearch_to_tsquery fuer flexiblere Suche und rankt nach Relevanz.
     * Gibt nur die relevantesten Empfehlungen zurueck (nicht alle 915).
     */
    @Query(value = """
            SELECT sr.* FROM wb_seg4_recommendations sr
            JOIN wb_knowledge_items ki ON ki.id = sr.knowledge_item_id
            WHERE ki.tenant_id = :tenantId
              AND to_tsvector('german',
                  COALESCE(sr.schlagworte,'') || ' ' || COALESCE(sr.problem_erlaeuterung,'') || ' ' ||
                  COALESCE(sr.empfehlung,'') || ' ' || COALESCE(sr.entscheidung,'') || ' ' ||
                  COALESCE(sr.zusatzhinweis,''))
                  @@ to_tsquery('german', :query)
            ORDER BY ts_rank_cd(
                to_tsvector('german',
                    COALESCE(sr.schlagworte,'') || ' ' || COALESCE(sr.problem_erlaeuterung,'') || ' ' ||
                    COALESCE(sr.empfehlung,'') || ' ' || COALESCE(sr.entscheidung,'') || ' ' ||
                    COALESCE(sr.zusatzhinweis,'')),
                to_tsquery('german', :query)) DESC
            LIMIT 20
            """, nativeQuery = true)
    List<Seg4Recommendation> fullTextSearch(@Param("tenantId") String tenantId, @Param("query") String query);
}
