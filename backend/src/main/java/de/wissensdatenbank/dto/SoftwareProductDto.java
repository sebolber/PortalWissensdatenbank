package de.wissensdatenbank.dto;

import java.time.LocalDateTime;

public record SoftwareProductDto(
        Long id,
        String name,
        String executableName,
        String publisher,
        String description,
        LocalDateTime createdAt
) {
}
