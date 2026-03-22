package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.Document;
import de.wissensdatenbank.enums.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    Page<Document> findByTenantId(String tenantId, Pageable pageable);

    Page<Document> findByTenantIdAndStatus(String tenantId, DocumentStatus status, Pageable pageable);

    Page<Document> findByTenantIdAndCategoryId(String tenantId, String categoryId, Pageable pageable);

    Optional<Document> findByIdAndTenantId(String id, String tenantId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, DocumentStatus status);

    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId ORDER BY d.createdAt DESC")
    List<Document> findNewest(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND d.status = 'PUBLISHED' ORDER BY d.viewCount DESC")
    List<Document> findMostViewed(@Param("tenantId") String tenantId, Pageable pageable);

    @Query("SELECT d FROM Document d WHERE d.tenantId = :tenantId AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Document> search(@Param("tenantId") String tenantId, @Param("query") String query, Pageable pageable);
}
