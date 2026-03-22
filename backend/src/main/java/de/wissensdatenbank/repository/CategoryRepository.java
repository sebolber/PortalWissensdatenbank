package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    List<Category> findByTenantIdOrderByOrderIndexAsc(String tenantId);

    Optional<Category> findByIdAndTenantId(String id, String tenantId);

    boolean existsByTenantIdAndNameAndParentId(String tenantId, String name, String parentId);

    long countByTenantId(String tenantId);
}
