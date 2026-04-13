package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.KnowledgeSubArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeSubArticleRepository extends JpaRepository<KnowledgeSubArticle, Long> {

    /** Alle Top-Level-Abschnitte eines KnowledgeItems (ohne Parent). */
    List<KnowledgeSubArticle> findByKnowledgeItemIdAndParentIsNullOrderByOrderIndexAsc(Long knowledgeItemId);

    /** Alle direkten Kinder eines Abschnitts. */
    List<KnowledgeSubArticle> findByParentIdOrderByOrderIndexAsc(Long parentId);

    /** Alle Abschnitte eines KnowledgeItems (flach). */
    List<KnowledgeSubArticle> findByKnowledgeItemIdOrderByOrderIndexAsc(Long knowledgeItemId);

    /** Abschnitt anhand Abschnittsnummer finden (z.B. "3.3.14.4.12"). */
    Optional<KnowledgeSubArticle> findByKnowledgeItemIdAndSectionNumber(Long knowledgeItemId, String sectionNumber);

    /** Alle Nachkommen eines Abschnitts über Materialized Path. */
    @Query("SELECT ksa FROM KnowledgeSubArticle ksa WHERE ksa.path LIKE CONCAT(:pathPrefix, '%') ORDER BY ksa.path")
    List<KnowledgeSubArticle> findDescendantsByPath(@Param("pathPrefix") String pathPrefix);

    /** Volltextsuche innerhalb eines KnowledgeItems. */
    @Query(value = """
            SELECT ksa.* FROM wb_knowledge_sub_articles ksa
            WHERE ksa.knowledge_item_id = :knowledgeItemId
              AND to_tsvector('german', COALESCE(ksa.heading,'') || ' ' || COALESCE(ksa.content,''))
                  @@ plainto_tsquery('german', :query)
            ORDER BY ksa.order_index
            """, nativeQuery = true)
    List<KnowledgeSubArticle> fullTextSearchWithinItem(
            @Param("knowledgeItemId") Long knowledgeItemId,
            @Param("query") String query);
}
