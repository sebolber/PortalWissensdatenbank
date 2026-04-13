package de.wissensdatenbank.dto;

import de.wissensdatenbank.enums.BindingLevel;
import de.wissensdatenbank.enums.KnowledgeType;

import java.time.LocalDateTime;
import java.util.List;

public record KnowledgeItemDto(
        Long id,
        String title,
        String summary,
        KnowledgeType knowledgeType,
        BindingLevel bindingLevel,
        String keywords,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        String sourceReference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        List<String> tags,
        int seg4RecommendationCount,
        Long productVersionId,
        String productVersionLabel,
        String productName
) {
    /** Abwaertskompatibiler Konstruktor ohne Produktversion-Felder. */
    public KnowledgeItemDto(
            Long id, String title, String summary,
            KnowledgeType knowledgeType, BindingLevel bindingLevel,
            String keywords, LocalDateTime validFrom, LocalDateTime validUntil,
            String sourceReference, LocalDateTime createdAt, LocalDateTime updatedAt,
            String createdBy, List<String> tags, int seg4RecommendationCount) {
        this(id, title, summary, knowledgeType, bindingLevel, keywords,
                validFrom, validUntil, sourceReference, createdAt, updatedAt,
                createdBy, tags, seg4RecommendationCount, null, null, null);
    }
}
