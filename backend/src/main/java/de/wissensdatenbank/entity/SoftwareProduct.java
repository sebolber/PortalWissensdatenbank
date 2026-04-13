package de.wissensdatenbank.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Softwareprodukt, dem Wissensartikel zugeordnet werden können (z.B. a1dlg.exe).
 */
@Entity
@Table(name = "wb_software_products",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name"}),
        indexes = @Index(name = "idx_wb_sp_tenant", columnList = "tenant_id"))
public class SoftwareProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "executable_name", length = 100)
    private String executableName;

    @Column(length = 300)
    private String publisher;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExecutableName() { return executableName; }
    public void setExecutableName(String executableName) { this.executableName = executableName; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
