package de.wissensdatenbank.dto;

import java.time.LocalDate;

public record ProductVersionDto(
        Long id,
        Long productId,
        String productName,
        String versionLabel,
        LocalDate releaseDate,
        String changeSummary
) {
}
