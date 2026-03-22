package de.wissensdatenbank.llm;

/**
 * Strukturierte LLM-Anfrage.
 */
public record LlmRequest(
        String tenantId,
        String jwtToken,
        String systemPrompt,
        String userPrompt
) {
}
