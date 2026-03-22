package de.wissensdatenbank.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentSuggestionDto(
        Long id,
        String fileName,
        String fileContentType,
        String status,
        String errorMessage,
        List<String> empfehlungen,
        String llmModel,
        int tokenCount,
        List<SuggestionResponse.UsedSource> quellen,
        Long auditLogId,
        String modelConfigId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
