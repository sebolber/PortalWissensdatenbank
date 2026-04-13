package de.wissensdatenbank.controller;

import de.wissensdatenbank.config.SecurityHelper;
import de.wissensdatenbank.dto.ProductVersionDto;
import de.wissensdatenbank.dto.SoftwareProductDto;
import de.wissensdatenbank.entity.ProductVersion;
import de.wissensdatenbank.entity.SoftwareProduct;
import de.wissensdatenbank.repository.ProductVersionRepository;
import de.wissensdatenbank.repository.SoftwareProductRepository;
import de.wissensdatenbank.service.PermissionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST-API fuer Softwareprodukte und deren Versionen.
 */
@RestController
@RequestMapping("/api/software-products")
public class SoftwareProductController {

    private final SoftwareProductRepository productRepository;
    private final ProductVersionRepository versionRepository;
    private final SecurityHelper securityHelper;
    private final PermissionService permissionService;

    public SoftwareProductController(SoftwareProductRepository productRepository,
                                      ProductVersionRepository versionRepository,
                                      SecurityHelper securityHelper,
                                      PermissionService permissionService) {
        this.productRepository = productRepository;
        this.versionRepository = versionRepository;
        this.securityHelper = securityHelper;
        this.permissionService = permissionService;
    }

    @GetMapping
    public List<SoftwareProductDto> listProducts() {
        permissionService.requireLesen();
        String tenantId = securityHelper.getCurrentTenantId();
        return productRepository.findByTenantId(tenantId).stream().map(this::toDto).toList();
    }

    @PostMapping
    public ResponseEntity<SoftwareProductDto> createProduct(@RequestBody SoftwareProductDto request) {
        permissionService.requireSchreiben();
        String tenantId = securityHelper.getCurrentTenantId();

        SoftwareProduct product = new SoftwareProduct();
        product.setTenantId(tenantId);
        product.setName(request.name());
        product.setExecutableName(request.executableName());
        product.setPublisher(request.publisher());
        product.setDescription(request.description());

        SoftwareProduct saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @GetMapping("/{productId}/versions")
    public List<ProductVersionDto> listVersions(@PathVariable Long productId) {
        permissionService.requireLesen();
        String tenantId = securityHelper.getCurrentTenantId();
        SoftwareProduct product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produkt nicht gefunden: " + productId));
        return versionRepository.findByProductId(product.getId()).stream()
                .map(v -> toVersionDto(v, product)).toList();
    }

    @PostMapping("/{productId}/versions")
    public ResponseEntity<ProductVersionDto> createVersion(@PathVariable Long productId,
                                                            @RequestBody ProductVersionDto request) {
        permissionService.requireSchreiben();
        String tenantId = securityHelper.getCurrentTenantId();
        SoftwareProduct product = productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produkt nicht gefunden: " + productId));

        ProductVersion version = new ProductVersion();
        version.setProduct(product);
        version.setVersionLabel(request.versionLabel());
        version.setReleaseDate(request.releaseDate());
        version.setChangeSummary(request.changeSummary());

        ProductVersion saved = versionRepository.save(version);
        return ResponseEntity.status(HttpStatus.CREATED).body(toVersionDto(saved, product));
    }

    private SoftwareProductDto toDto(SoftwareProduct p) {
        return new SoftwareProductDto(p.getId(), p.getName(), p.getExecutableName(),
                p.getPublisher(), p.getDescription(), p.getCreatedAt());
    }

    private ProductVersionDto toVersionDto(ProductVersion v, SoftwareProduct p) {
        return new ProductVersionDto(v.getId(), p.getId(), p.getName(),
                v.getVersionLabel(), v.getReleaseDate(), v.getChangeSummary());
    }
}
