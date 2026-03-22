package de.wissensdatenbank.llm;

/**
 * Strukturierte LLM-Anfrage.
 */
public record LlmRequest(
        String tenantId,
        String jwtToken,
        String configId,
        String systemPrompt,
        String userPrompt,
        Integer maxTokens
) {
    public LlmRequest(String tenantId, String jwtToken, String configId,
                      String systemPrompt, String userPrompt) {
        this(tenantId, jwtToken, configId, systemPrompt, userPrompt, null);
    }
}
