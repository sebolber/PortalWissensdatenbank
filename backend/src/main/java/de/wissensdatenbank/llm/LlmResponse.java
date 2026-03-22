package de.wissensdatenbank.llm;

/**
 * Antwort vom LLM inkl. Metadaten.
 */
public record LlmResponse(
        String content,
        String model,
        String configId,
        int tokenCount
) {
}
