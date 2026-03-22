package de.wissensdatenbank.dto;

public record StatistikDto(
    long totalDocuments,
    long publishedDocuments,
    long draftDocuments,
    long archivedDocuments,
    long totalCategories,
    long totalTags
) {}
