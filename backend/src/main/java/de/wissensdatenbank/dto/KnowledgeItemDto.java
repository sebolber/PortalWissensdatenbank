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
        int seg4RecommendationCount
) {
}
