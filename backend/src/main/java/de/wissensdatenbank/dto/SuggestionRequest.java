package de.wissensdatenbank.dto;

import java.util.List;

public record SuggestionRequest(
        String dokumentText,
        List<String> diagnosen,
        List<String> massnahmen
) {
}
