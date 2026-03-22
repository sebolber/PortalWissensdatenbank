package de.wissensdatenbank.dto;

import de.wissensdatenbank.enums.DocumentStatus;
import java.time.LocalDateTime;
import java.util.List;

public record DocumentDto(
    String id,
    String tenantId,
    String title,
    String content,
    String summary,
    DocumentStatus status,
    String categoryId,
    String categoryName,
    List<TagDto> tags,
    String createdBy,
    String updatedBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    int version,
    boolean publicWithinTenant,
    int viewCount,
    double averageRating,
    int ratingCount
) {}
