package de.wissensdatenbank.repository;

import de.wissensdatenbank.entity.ProductVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVersionRepository extends JpaRepository<ProductVersion, Long> {

    List<ProductVersion> findByProductId(Long productId);

    Optional<ProductVersion> findByProductIdAndVersionLabel(Long productId, String versionLabel);
}
