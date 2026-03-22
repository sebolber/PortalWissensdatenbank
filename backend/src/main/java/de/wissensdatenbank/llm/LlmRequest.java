package de.wissensdatenbank.llm;

/**
 * Strukturierte LLM-Anfrage.
 */
public record LlmRequest(
        String tenantId,
        String jwtToken,
        String configId,
        String systemPrompt,
        String userPrompt
) {
}
