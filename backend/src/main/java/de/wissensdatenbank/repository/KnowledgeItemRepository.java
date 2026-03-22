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
     * PostgreSQL Volltext-Suche (deutsch) über title, summary, keywords.
     */
    @Query(value = """
            SELECT * FROM wb_knowledge_items
            WHERE tenant_id = :tenantId
              AND to_tsvector('german', COALESCE(title,'') || ' ' || COALESCE(summary,'') || ' ' || COALESCE(keywords,''))
                  @@ plainto_tsquery('german', :query)
            """, nativeQuery = true)
    List<KnowledgeItem> fullTextSearch(@Param("tenantId") String tenantId, @Param("query") String query);
}
