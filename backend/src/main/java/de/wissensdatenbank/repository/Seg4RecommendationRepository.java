package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.Seg4Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Seg4RecommendationRepository extends JpaRepository<Seg4Recommendation, Long> {

    List<Seg4Recommendation> findByKnowledgeItemId(Long knowledgeItemId);

    List<Seg4Recommendation> findByRecommendationNumberContainingIgnoreCase(String number);
}
