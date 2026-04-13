package de.wissensdatenbank.dto;

import java.util.List;

/**
 * Rekursiver Baum-DTO fuer hierarchische SubArticle-Strukturen.
 * Wird fuer die Darstellung von Handbuch-Inhaltsverzeichnissen verwendet.
 */
public record KnowledgeSubArticleTreeDto(
        Long id,
        String heading,
        String sectionNumber,
        int depth,
        int orderIndex,
        String contentPreview,
        List<KnowledgeSubArticleTreeDto> children
) {
}
