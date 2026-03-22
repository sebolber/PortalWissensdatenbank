package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.SuggestionAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestionAuditLogRepository extends JpaRepository<SuggestionAuditLog, Long> {

    Page<SuggestionAuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
}
