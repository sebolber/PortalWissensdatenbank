package de.wissensdatenbank.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Löst die aktive LLM-Konfiguration eines Mandanten über PortalCore auf.
 * Speichert nur die Referenz-ID, nie das Modell selbst.
 */
@Service
public class LlmModelResolutionService {

    private static final Logger log = LoggerFactory.getLogger(LlmModelResolutionService.class);

    private final RestClient restClient;

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    public LlmModelResolutionService() {
        this.restClient = RestClient.create();
    }

    /**
     * Prüft, ob der Mandant ein LLM konfiguriert hat.
     */
    @SuppressWarnings("unchecked")
    public boolean isLlmAvailable(String tenantId, String jwtToken) {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(portalCoreBaseUrl + "/api/tenants/" + tenantId + "/profile/llm/status")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .body(Map.class);

            return response != null && Boolean.TRUE.equals(response.get("active"));
        } catch (Exception e) {
            log.warn("LLM-Status-Abfrage fehlgeschlagen fuer Mandant {}: {}", tenantId, e.getMessage());
            return false;
        }
    }

    /**
     * Ruft alle LLM-Konfigurationen des Mandanten von PortalCore ab.
     */
    public List<Map<String, Object>> listLlmModels(String tenantId, String jwtToken) {
        try {
            List<Map<String, Object>> configs = restClient.get()
                    .uri(portalCoreBaseUrl + "/api/tenants/" + tenantId + "/profile/llm")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (configs == null) return List.of();

            return configs.stream()
                    .map(c -> Map.<String, Object>of(
                            "id", c.getOrDefault("id", ""),
                            "name", c.getOrDefault("name", ""),
                            "provider", c.getOrDefault("provider", ""),
                            "model", c.getOrDefault("model", ""),
                            "isActive", c.getOrDefault("isActive", false)
                    ))
                    .toList();
        } catch (Exception e) {
            log.warn("LLM-Modell-Abfrage fehlgeschlagen fuer Mandant {}: {}", tenantId, e.getMessage());
            return List.of();
        }
    }
}
