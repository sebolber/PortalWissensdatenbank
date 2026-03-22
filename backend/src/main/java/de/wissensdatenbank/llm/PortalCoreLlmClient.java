package de.wissensdatenbank.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * LLM-Client, der alle Aufrufe an PortalCore's LLM-Proxy delegiert.
 * Keine eigene Modellverwaltung – API-Keys bleiben in PortalCore.
 */
@Service
public class PortalCoreLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(PortalCoreLlmClient.class);

    private final RestClient restClient;

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    public PortalCoreLlmClient() {
        this.restClient = RestClient.create();
    }

    @Override
    @SuppressWarnings("unchecked")
    public LlmResponse chat(LlmRequest request) {
        Map<String, Object> body = Map.of(
                "systemPrompt", request.systemPrompt(),
                "messages", List.of(
                        Map.of("role", "user", "content", request.userPrompt())
                )
        );

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri(portalCoreBaseUrl + "/api/tenants/" + request.tenantId() + "/profile/llm/chat")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + request.jwtToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            log.error("LLM-Proxy HTTP-Fehler {}: {}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new LlmException(
                    "LLM-Aufruf fehlgeschlagen (HTTP " + e.getStatusCode().value() + "): "
                            + extractErrorMessage(e.getResponseBodyAsString()), e);
        } catch (Exception e) {
            log.error("LLM-Proxy-Aufruf fehlgeschlagen: {}", e.getMessage(), e);
            throw new LlmException(
                    "Kein LLM konfiguriert oder LLM-Aufruf fehlgeschlagen. "
                            + "Bitte pruefen Sie die KI-Verbindung in den Mandanteneinstellungen.", e);
        }

        if (response == null) {
            throw new LlmException("Keine Antwort vom LLM-Service erhalten.");
        }

        String content = (String) response.get("content");
        String model = (String) response.get("model");
        String configId = (String) response.get("configId");
        int tokenCount = response.get("tokenCount") != null
                ? ((Number) response.get("tokenCount")).intValue() : 0;

        if (content == null || content.isBlank()) {
            throw new LlmException("Leere Antwort vom LLM erhalten.");
        }

        return new LlmResponse(content, model, configId, tokenCount);
    }

    @SuppressWarnings("unchecked")
    private String extractErrorMessage(String responseBody) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> errorMap = mapper.readValue(responseBody, Map.class);
            Object error = errorMap.get("error");
            if (error != null) {
                return error.toString();
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }
}
