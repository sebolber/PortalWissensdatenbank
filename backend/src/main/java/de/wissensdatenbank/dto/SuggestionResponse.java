package de.wissensdatenbank.dto;

import java.util.List;

public record SuggestionResponse(
        List<String> empfehlungen,
        String llmModel,
        int tokenCount,
        List<UsedSource> quellen,
        Long auditLogId
) {
    public record UsedSource(Long id, String title, String bindingLevel, String matchReason) {}
}
