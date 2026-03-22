package de.wissensdatenbank.retrieval;

import java.util.List;

/**
 * Strukturierte Suchanfrage, die aus einem Behandlungsdokument extrahiert wurde.
 */
public record SearchQuery(
        String freitextQuery,
        List<String> diagnosen,
        List<String> massnahmen,
        List<String> keywords,
        String tenantId
) {
    public SearchQuery {
        if (diagnosen == null) diagnosen = List.of();
        if (massnahmen == null) massnahmen = List.of();
        if (keywords == null) keywords = List.of();
    }
}
