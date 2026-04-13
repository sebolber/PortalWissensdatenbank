package de.wissensdatenbank.dto;

import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.enums.KnowledgeType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request-DTO zum Erstellen und Aktualisieren von KnowledgeItems.
 */
public record KnowledgeItemCreateRequest(
        String title,
        String summary,
        KnowledgeType knowledgeType,
        BindingLevel bindingLevel,
        String keywords,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        String sourceReference,
        Long productVersionId,
        List<String> tags,
        List<SubArticleRequest> subArticles
) {
    /**
     * Rekursiver Request fuer hierarchische SubArticles.
     */
    public record SubArticleRequest(
            String heading,
            String content,
            String sectionNumber,
            int orderIndex,
            List<SubArticleRequest> children
    ) {
    }
}
