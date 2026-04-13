package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.KnowledgeCrossReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeCrossReferenceRepository extends JpaRepository<KnowledgeCrossReference, Long> {

    /** Alle ausgehenden Verweise eines KnowledgeItems. */
    List<KnowledgeCrossReference> findBySourceItemId(Long sourceItemId);

    /** Alle eingehenden Verweise auf ein KnowledgeItem. */
    List<KnowledgeCrossReference> findByTargetItemId(Long targetItemId);

    /** Alle ausgehenden Verweise eines bestimmten SubArticles. */
    List<KnowledgeCrossReference> findBySourceSubArticleId(Long sourceSubArticleId);
}
