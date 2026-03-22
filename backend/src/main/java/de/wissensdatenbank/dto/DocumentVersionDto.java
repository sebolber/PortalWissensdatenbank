package de.wissensdatenbank.dto;

import java.time.LocalDateTime;

public record DocumentVersionDto(
    String id,
    int version,
    String title,
    String summary,
    String changedBy,
    LocalDateTime changedAt,
    String changeNote
) {}
