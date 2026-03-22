package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, String> {

    Optional<Feedback> findByDocumentIdAndUserId(String documentId, String userId);
}
