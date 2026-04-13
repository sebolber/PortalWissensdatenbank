package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.SoftwareProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SoftwareProductRepository extends JpaRepository<SoftwareProduct, Long> {

    List<SoftwareProduct> findByTenantId(String tenantId);

    Optional<SoftwareProduct> findByIdAndTenantId(Long id, String tenantId);

    Optional<SoftwareProduct> findByTenantIdAndName(String tenantId, String name);
}
