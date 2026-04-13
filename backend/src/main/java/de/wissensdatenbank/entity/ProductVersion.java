package de.wissensdatenbank.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Version eines Softwareprodukts (z.B. Version 152.0 des Änderungsdialogs).
 */
@Entity
@Table(name = "wb_product_versions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "version_label"}),
        indexes = @Index(name = "idx_wb_pv_product", columnList = "product_id"))
public class ProductVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private SoftwareProduct product;

    @Column(name = "version_label", nullable = false, length = 100)
    private String versionLabel;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SoftwareProduct getProduct() { return product; }
    public void setProduct(SoftwareProduct product) { this.product = product; }

    public String getVersionLabel() { return versionLabel; }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
