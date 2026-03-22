package de.wissensdatenbank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Registriert Dashboard-Widgets im PortalCore beim Anwendungsstart.
 */
@Service
public class WidgetRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(WidgetRegistrationService.class);

    private final RestClient restClient;

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    @Value("${portal.widget.registration-enabled:false}")
    private boolean registrationEnabled;

    public WidgetRegistrationService() {
        this.restClient = RestClient.create();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWidgets() {
        if (!registrationEnabled) {
            log.info("Widget-Registrierung deaktiviert, ueberspringe.");
            return;
        }

        registerWidget("wb-neueste-dokumente", "Neueste Dokumente",
                "Zeigt die neuesten Dokumente der Wissensdatenbank an",
                "Wissensdatenbank", "/api/dokumente/neueste?limit=5",
                "/dokumente", 2, 2);

        registerWidget("wb-beliebte-dokumente", "Beliebte Dokumente",
                "Zeigt die meistgelesenen Dokumente an",
                "Wissensdatenbank", "/api/dokumente/beliebt?limit=5",
                "/dokumente", 2, 2);

        registerWidget("wb-statistik", "Wissensdatenbank Statistik",
                "Zeigt Statistiken zur Wissensdatenbank",
                "Wissensdatenbank", "/api/dokumente/statistik",
                "/dokumente", 2, 1);

        log.info("Widget-Registrierung abgeschlossen.");
    }

    private void registerWidget(String widgetKey, String titel, String beschreibung,
                                 String kategorie, String datenEndpunkt,
                                 String linkZiel, int breite, int hoehe) {
        try {
            Map<String, Object> widget = Map.ofEntries(
                    Map.entry("id", UUID.randomUUID().toString()),
                    Map.entry("widgetKey", widgetKey),
                    Map.entry("titel", titel),
                    Map.entry("beschreibung", beschreibung),
                    Map.entry("kategorie", kategorie),
                    Map.entry("widgetTyp", "DATA_LIST"),
                    Map.entry("appId", "wissensdatenbank"),
                    Map.entry("appName", "Wissensdatenbank"),
                    Map.entry("standardBreite", breite),
                    Map.entry("standardHoehe", hoehe),
                    Map.entry("minBreite", 1),
                    Map.entry("minHoehe", 1),
                    Map.entry("maxBreite", 4),
                    Map.entry("maxHoehe", 4),
                    Map.entry("datenEndpunkt", datenEndpunkt),
                    Map.entry("linkZiel", linkZiel),
                    Map.entry("aktiv", true),
                    Map.entry("konfigurierbar", false)
            );

            restClient.post()
                    .uri(portalCoreBaseUrl + "/api/dashboard/widgets/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(widget)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Widget registriert: {}", widgetKey);
        } catch (Exception e) {
            log.warn("Widget-Registrierung fehlgeschlagen '{}': {}", widgetKey, e.getMessage());
        }
    }
}
