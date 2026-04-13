package de.wissensdatenbank.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${portal.jwt.secret:}")
    private String secret;

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    private final RestClient restClient = RestClient.create();

    private boolean hasLocalSecret() {
        return secret != null && !secret.isBlank() && secret.length() >= 32;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (hasLocalSecret()) {
            authenticateLocally(token);
        } else {
            authenticateViaPortalCore(token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Lokale JWT-Validierung wenn JWT_SECRET verfuegbar.
     */
    private void authenticateLocally(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration().before(new Date())) {
                return;
            }

            String userId = claims.getSubject();
            String tenantId = claims.get("tenantId", String.class);
            String email = claims.get("email", String.class);

            setAuthentication(userId, tenantId, email, token);

        } catch (JwtException e) {
            log.warn("JWT-Validierung fehlgeschlagen: {}", e.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("JWT-Authentifizierung fehlgeschlagen: {}", e.getMessage());
        }
    }

    /**
     * Token-Validierung ueber Portal-Core wenn kein lokales JWT_SECRET vorhanden.
     * Ruft /api/auth/me am Portal-Core auf, um Benutzerinfos aus dem Token zu erhalten.
     */
    @SuppressWarnings("unchecked")
    private void authenticateViaPortalCore(String token) {
        try {
            Map<String, Object> userInfo = restClient.get()
                    .uri(portalCoreBaseUrl + "/api/auth/me")
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(Map.class);

            if (userInfo == null) {
                return;
            }

            String userId = (String) userInfo.get("id");
            String tenantId = (String) userInfo.get("tenantId");
            String email = (String) userInfo.get("email");

            if (userId == null) {
                // Fallback: Subject aus unverified Token Claims lesen
                userId = extractUnverifiedSubject(token);
                tenantId = extractUnverifiedClaim(token, "tenantId");
                email = extractUnverifiedClaim(token, "email");
            }

            setAuthentication(userId, tenantId, email, token);

        } catch (Exception e) {
            log.warn("Portal-Core Authentifizierung fehlgeschlagen: {}", e.getMessage());
        }
    }

    private void setAuthentication(String userId, String tenantId, String email, String token) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, token, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        auth.setDetails(new AuthDetails(userId, tenantId, email));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * Liest den Subject aus einem JWT ohne Signaturpruefung
     * (nur als Fallback wenn Portal-Core keine User-Info liefert).
     */
    private String extractUnverifiedSubject(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]),
                        StandardCharsets.UTF_8);
                var map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, Map.class);
                return (String) map.get("sub");
            }
        } catch (Exception e) {
            log.debug("Konnte unverifizierten Subject nicht lesen: {}", e.getMessage());
        }
        return null;
    }

    private String extractUnverifiedClaim(String token, String claim) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]),
                        StandardCharsets.UTF_8);
                var map = new com.fasterxml.jackson.databind.ObjectMapper().readValue(payload, Map.class);
                return (String) map.get(claim);
            }
        } catch (Exception e) {
            log.debug("Konnte unverifizierten Claim '{}' nicht lesen: {}", claim, e.getMessage());
        }
        return null;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("portal_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public record AuthDetails(String userId, String tenantId, String email) {}
}
