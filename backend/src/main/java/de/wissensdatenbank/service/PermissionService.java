package de.wissensdatenbank.service;

import de.wissensdatenbank.config.SecurityHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    static final String UC_LESEN = "wissensdatenbank-lesen";
    static final String UC_SCHREIBEN = "wissensdatenbank-schreiben";
    static final String UC_VEROEFFENTLICHEN = "wissensdatenbank-veroeffentlichen";
    static final String UC_ADMIN = "wissensdatenbank-admin";

    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final SecurityHelper securityHelper;
    private final RestClient restClient;
    private final String portalCoreBaseUrl;

    private final ConcurrentHashMap<String, CachedPermissions> cache = new ConcurrentHashMap<>();

    public PermissionService(SecurityHelper securityHelper,
                             @Value("${portal.core.base-url:http://portal-backend:8080}") String portalCoreBaseUrl) {
        this.securityHelper = securityHelper;
        this.restClient = RestClient.create();
        this.portalCoreBaseUrl = portalCoreBaseUrl;
    }

    public void requireLesen() {
        requirePermission(UC_LESEN, "anzeigen");
    }

    public void requireSchreiben() {
        requirePermission(UC_SCHREIBEN, "schreiben");
    }

    public void requireVeroeffentlichen() {
        requirePermission(UC_VEROEFFENTLICHEN, "schreiben");
    }

    public void requireAdmin() {
        requirePermission(UC_ADMIN, "schreiben");
    }

    @SuppressWarnings("unchecked")
    private void requirePermission(String useCase, String typ) {
        String token = securityHelper.getCurrentToken();
        String userId = securityHelper.getCurrentUserId();

        CachedPermissions cached = cache.get(userId);
        if (cached != null && !cached.isExpired()) {
            if (cached.isSuperAdmin || cached.hasPermission(useCase, typ)) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Berechtigung");
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri(portalCoreBaseUrl + "/api/apps/portalwissensdatenbank/berechtigungen")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Berechtigungspruefung fehlgeschlagen");
            }

            boolean superAdmin = Boolean.TRUE.equals(response.get("superAdmin"));
            List<Map<String, Object>> permissions = (List<Map<String, Object>>) response.get("permissions");

            CachedPermissions newCache = new CachedPermissions(superAdmin, permissions);
            cache.put(userId, newCache);

            if (superAdmin || newCache.hasPermission(useCase, typ)) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Keine Berechtigung");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Berechtigungspruefung fehlgeschlagen: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Berechtigungspruefung fehlgeschlagen");
        }
    }

    public void invalidateCache() {
        cache.clear();
    }

    private static class CachedPermissions {
        final boolean isSuperAdmin;
        final List<Map<String, Object>> permissions;
        final long createdAt;

        CachedPermissions(boolean isSuperAdmin, List<Map<String, Object>> permissions) {
            this.isSuperAdmin = isSuperAdmin;
            this.permissions = permissions != null ? permissions : List.of();
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > CACHE_TTL_MS;
        }

        boolean hasPermission(String useCase, String typ) {
            return permissions.stream()
                    .filter(p -> useCase.equals(p.get("useCase")))
                    .anyMatch(p -> Boolean.TRUE.equals(p.get(typ)));
        }
    }
}
