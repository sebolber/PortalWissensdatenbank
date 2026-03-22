package de.wissensdatenbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DocumentCreateRequest(
    @NotBlank @Size(max = 500) String title,
    @NotBlank String content,
    @Size(max = 2000) String summary,
    String categoryId,
    List<String> tagIds,
    boolean publicWithinTenant
) {
    public DocumentCreateRequest {
        if (publicWithinTenant == false) {
            // default to true
        }
    }
}
