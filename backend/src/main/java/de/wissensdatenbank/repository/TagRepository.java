package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {

    List<Tag> findByTenantIdOrderByNameAsc(String tenantId);

    Optional<Tag> findByIdAndTenantId(String id, String tenantId);

    boolean existsByTenantIdAndName(String tenantId, String name);

    long countByTenantId(String tenantId);

    List<Tag> findByTenantIdAndNameIn(String tenantId, List<String> names);
}
