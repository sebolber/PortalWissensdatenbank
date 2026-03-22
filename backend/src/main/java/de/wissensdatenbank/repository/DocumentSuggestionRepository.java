package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.DocumentSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentSuggestionRepository extends JpaRepository<DocumentSuggestion, Long> {

    List<DocumentSuggestion> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<DocumentSuggestion> findByIdAndTenantId(Long id, String tenantId);
}
