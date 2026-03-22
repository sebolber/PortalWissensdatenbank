package de.wissensdatenbank.llm;

/**
 * Interface für LLM-Aufrufe. Implementierung delegiert an PortalCore.
 * Keine eigene Modellverwaltung – Modelle kommen aus PortalCore.
 */
public interface LlmClient {

    /**
     * Sendet einen Chat-Request an das LLM via PortalCore.
     *
     * @param request strukturierte Anfrage
     * @return LLM-Antwort inkl. Modell-Info und Token-Count
     */
    LlmResponse chat(LlmRequest request);
}
