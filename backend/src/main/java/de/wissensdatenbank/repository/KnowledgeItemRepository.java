package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.KnowledgeItem;
import de.wissensdatenbank.enums.KnowledgeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long> {

    Page<KnowledgeItem> findByTenantId(String tenantId, Pageable pageable);

    Page<KnowledgeItem> findByTenantIdAndKnowledgeType(String tenantId, KnowledgeType type, Pageable pageable);

    Optional<KnowledgeItem> findByIdAndTenantId(Long id, String tenantId);

    /**
     * PostgreSQL Volltext-Suche (deutsch) über title, summary, keywords,
     * SEG4-Empfehlungsinhalte und SubArticle-Inhalte (heading, content).
     */
    @Query(value = """
            SELECT DISTINCT ki.* FROM wb_knowledge_items ki
            LEFT JOIN wb_seg4_recommendations sr ON sr.knowledge_item_id = ki.id
            LEFT JOIN wb_knowledge_sub_articles ksa ON ksa.knowledge_item_id = ki.id
            WHERE ki.tenant_id = :tenantId
              AND (
                to_tsvector('german', COALESCE(ki.title,'') || ' ' || COALESCE(ki.summary,'') || ' ' || COALESCE(ki.keywords,''))
                    @@ plainto_tsquery('german', :query)
                OR to_tsvector('german',
                    COALESCE(sr.schlagworte,'') || ' ' || COALESCE(sr.problem_erlaeuterung,'') || ' ' ||
                    COALESCE(sr.empfehlung,'') || ' ' || COALESCE(sr.entscheidung,'') || ' ' ||
                    COALESCE(sr.zusatzhinweis,''))
                    @@ plainto_tsquery('german', :query)
                OR to_tsvector('german', COALESCE(ksa.heading,'') || ' ' || COALESCE(ksa.content,''))
                    @@ plainto_tsquery('german', :query)
              )
            """, nativeQuery = true)
    List<KnowledgeItem> fullTextSearch(@Param("tenantId") String tenantId, @Param("query") String query);

    List<KnowledgeItem> findByTenantIdAndProductVersionId(String tenantId, Long productVersionId);
}
