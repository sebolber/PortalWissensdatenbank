package de.wissensdatenbank.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Request-DTO fuer den Bulk-Import eines Benutzerhandbuchs.
 * Erstellt ein SoftwareProduct, eine ProductVersion und mehrere KnowledgeItems
 * mit hierarchischen SubArticles in einem Vorgang.
 */
public record HandbuchImportRequest(
        SoftwareProductInput softwareProduct,
        ProductVersionInput productVersion,
        List<KnowledgeItemCreateRequest> knowledgeItems
) {
    public record SoftwareProductInput(
            String name,
            String executableName,
            String publisher,
            String description
    ) {}

    public record ProductVersionInput(
            String versionLabel,
            LocalDate releaseDate,
            String changeSummary
    ) {}
}
