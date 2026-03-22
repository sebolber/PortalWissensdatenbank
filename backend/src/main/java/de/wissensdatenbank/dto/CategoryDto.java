package de.wissensdatenbank.dto;

import java.time.LocalDateTime;

public record CategoryDto(
    String id,
    String name,
    String description,
    String parentId,
    int orderIndex,
    LocalDateTime createdAt
) {}
