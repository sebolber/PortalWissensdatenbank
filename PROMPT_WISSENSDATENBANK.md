# Prompt: Portal App "Wissensdatenbank" erstellen

> Dieser Prompt basiert auf der Analyse der bestehenden Portal-App "Wissensmanagement" und beschreibt alle Integrationsmuster, damit die neue App "Wissensdatenbank" korrekt ins Portal eingebunden wird.

---

## Aufgabe

Erstelle eine neue Portal-App namens **"Wissensdatenbank"** fuer das Health Portal (PortalCore). Die App soll als eigenstaendiger Microservice laufen und sich nahtlos in das Portal integrieren — genau wie die bestehende App "Wissensmanagement".

---

## 1. Architektur-Ueberblick

### Tech-Stack (identisch mit PortalCore)
- **Backend:** Spring Boot 3.2.4, Java 21, PostgreSQL 16, Flyway
- **Frontend:** Angular 18, TypeScript 5.4, Standalone Components
- **Auth:** JWT (Shared Secret mit PortalCore)
- **Deployment:** Docker (Multi-Stage Build), wird vom Portal per AppStore deployt
- **Port:** 8080 (intern im Container)

### Verzeichnisstruktur
```
PortalWissensdatenbank/
├── portal-app.yaml              # App-Manifest (PFLICHT - wird vom Portal gelesen)
├── Dockerfile                   # Multi-Stage Build (Angular + Spring Boot)
├── backend/
│   ├── pom.xml
│   └── src/main/java/de/wissensdatenbank/
│       ├── config/              # Security, JWT, CORS, SPA-Filter
│       ├── controller/          # REST-Endpunkte
│       ├── service/             # Business-Logik + WidgetRegistrationService
│       ├── repository/          # Spring Data JPA
│       ├── entity/              # JPA-Entities
│       └── dto/                 # Request/Response DTOs
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/        # Flyway-Migrationen mit Prefix "wb_"
├── frontend/
│   ├── package.json
│   ├── angular.json
│   └── src/app/
│       ├── app.component.ts     # Root mit iframe-Erkennung
│       ├── app.config.ts        # Router + HTTP-Interceptors
│       ├── app.routes.ts        # Routen-Definition
│       ├── interceptors/        # Portal-Auth-Interceptor
│       ├── services/            # API-Services
│       ├── models/              # TypeScript Interfaces
│       └── pages/               # Feature-Seiten
```

---

## 2. Portal-App-Manifest (`portal-app.yaml`)

Diese Datei ist das **zentrale Integrationsdokument**. Sie MUSS im Root des Repositories liegen. Das Portal liest sie automatisch beim Deployment.

```yaml
# portal-app.yaml - Manifest fuer Health Portal Apps
name: Wissensdatenbank

version: 1.0.0
backendVersion: 1.0.0
frontendVersion: 1.0.0

description: Zentrale Wissensdatenbank mit Dokumentenverwaltung, Suche und Kategorisierung

dockerfile: Dockerfile

port: 8080

healthCheck: /api/health

env:
  DB_URL: jdbc:postgresql://portal-db:5432/portal
  DB_USER: portal
  DB_PASS: portal

# Seiten die im Portal-Sidebar als Submenue erscheinen
# - route: Wird als ?page=<route> an den iframe uebergeben
# - useCase: Verknuepft die Seite mit einer Berechtigung (siehe useCases unten)
# - Nur Seiten mit useCase werden berechtigungsgesteuert angezeigt
pages:
  - label: "Startseite"
    route: "/"
    icon: "home"
  - label: "Dokumente"
    route: "/dokumente"
    icon: "book"
    useCase: "wissensdatenbank-lesen"
  - label: "Neues Dokument"
    route: "/dokumente/neu"
    icon: "plus"
    useCase: "wissensdatenbank-schreiben"
  - label: "Kategorien"
    route: "/kategorien"
    icon: "folder"
    useCase: "wissensdatenbank-admin"
  - label: "Konfiguration"
    route: "/konfiguration"
    icon: "settings"
    useCase: "wissensdatenbank-admin"

# Use Cases = Berechtigungen der App
# Werden beim Installieren automatisch ins Portal-RBAC-System uebernommen.
# Administratoren erhalten automatisch alle Rechte.
# Andere Gruppen koennen Rechte ueber die Gruppenverwaltung konfigurieren.
useCases:
  - key: wissensdatenbank-lesen
    label: "Wissensdatenbank - Lesen"
    beschreibung: "Lesezugriff auf Dokumente"
  - key: wissensdatenbank-schreiben
    label: "Wissensdatenbank - Schreiben"
    beschreibung: "Dokumente erstellen und bearbeiten"
  - key: wissensdatenbank-veroeffentlichen
    label: "Wissensdatenbank - Veroeffentlichen"
    beschreibung: "Dokumente veroeffentlichen und archivieren"
  - key: wissensdatenbank-admin
    label: "Wissensdatenbank - Administration"
    beschreibung: "Kategorien, Tags und Einstellungen verwalten"
```

### Wie Use Cases / Berechtigungen funktionieren

1. Die `useCases` aus `portal-app.yaml` werden beim Installieren der App automatisch als `AppUseCase`-Eintraege in der PortalCore-Datenbank gespeichert (Tabelle `app_use_cases`).
2. PortalCore-Administratoren koennen diese Use Cases dann Gruppen zuweisen (Tabelle `gruppen_berechtigungen`).
3. SuperAdmins haben automatisch Zugriff auf alle Use Cases.
4. Die `pages` mit `useCase`-Referenz werden im Portal-Sidebar nur angezeigt, wenn der Benutzer die entsprechende Berechtigung hat (`getVisiblePages(app)` in der Sidebar-Komponente).
5. Der `requireAppPermission`-Guard im Portal prueft beim Oeffnen der App, ob der Benutzer mindestens eine Berechtigung fuer die App hat.

---

## 3. Iframe-Integration

### Wie das Portal Apps einbettet

PortalCore bettet Apps per **iframe** ein. Die Route `/app/:appId` laedt die `AppViewerComponent`:

```
Portal-URL: https://portal.example.com/app/wissensdatenbank?page=/dokumente
                                          ↓
                        iframe src: http://wissensdatenbank:8080/?page=/dokumente
```

**Wichtige Details:**
- Der iframe hat `sandbox="allow-scripts allow-same-origin allow-forms allow-popups allow-popups-to-escape-sandbox allow-modals"`
- Die Seiten-Navigation erfolgt ueber den Query-Parameter `?page=<route>`
- Die App laeuft im **same-origin** Kontext (dank Docker-Netzwerk + nginx Reverse Proxy)

### Was die App tun muss

#### a) Iframe-Erkennung in `app.component.ts`

```typescript
@Component({
  selector: 'app-root',
  template: `
    <div class="app-layout" [class.embedded]="embedded">
      <!-- Sidebar nur anzeigen wenn NICHT im iframe -->
      <aside class="sidebar" *ngIf="!embedded">
        <!-- Eigene Navigation -->
      </aside>
      <main class="main-content" [class.embedded]="embedded">
        <router-outlet></router-outlet>
      </main>
    </div>
  `
})
export class AppComponent implements OnInit {
  embedded = false;

  ngOnInit(): void {
    // Erkennen ob die App im Portal-iframe laeuft
    this.embedded = window.self !== window.top;

    // Seite aus Query-Parameter navigieren (vom Portal gesetzt)
    const urlParams = new URLSearchParams(window.location.search);
    const page = urlParams.get('page');
    if (page) {
      this.router.navigateByUrl(page);
    }
  }
}
```

**Wichtig:** Im eingebetteten Modus wird die eigene Sidebar ausgeblendet, weil die Navigation ueber das Portal-Sidebar laeuft.

#### b) Portal-Auth-Interceptor

```typescript
// interceptors/portal-auth.interceptor.ts
export const portalAuthInterceptor: HttpInterceptorFn = (req, next) => {
  // Nur API-Requests authentifizieren
  if (!req.url.includes('api/')) {
    return next(req);
  }

  // JWT aus localStorage lesen (vom PortalCore unter 'portal_token' gespeichert)
  // Dank same-origin ist localStorage im iframe zugaenglich
  const token = localStorage.getItem('portal_token');
  if (token) {
    const authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(authReq);
  }

  return next(req);
};
```

#### c) App-Config registriert den Interceptor

```typescript
// app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([portalAuthInterceptor])),
  ]
};
```

---

## 4. Backend Security-Konfiguration

### JWT-Authentication-Filter

Die App teilt das **gleiche JWT-Secret** mit PortalCore. Das Token wird aus dem `Authorization: Bearer <token>` Header ODER aus dem Cookie `portal_token` gelesen.

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${portal.jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String tenantId = claims.get("tenantId", String.class);
            String email = claims.get("email", String.class);

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            auth.setDetails(new AuthDetails(userId, tenantId, email));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException e) {
            log.warn("JWT-Validierung fehlgeschlagen: {}", e.getClass().getSimpleName());
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Authorization Header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 2. Fallback: Cookie (fuer iframe-Zugriff via Portal)
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
```

### Security-Config

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // WICHTIG: Frame-Options deaktivieren, damit iframe funktioniert!
            .headers(headers -> headers.frameOptions(fo -> fo.disable()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/", "/index.html", "/assets/**", "/*.js", "/*.css", "/*.ico").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**Kritische Punkte:**
- `frameOptions(fo -> fo.disable())` — ohne das kann die App nicht im iframe geladen werden!
- CORS mit `allowCredentials(true)` und `allowedOriginPatterns(List.of("*"))`
- Stateless Sessions (kein Server-Side-State, alles ueber JWT)

### SecurityHelper

Hilfskomponente um in Services an User-/Tenant-Informationen zu kommen:

```java
@Component
public class SecurityHelper {
    public String getCurrentUserId() { ... }
    public String getCurrentTenantId() { ... }
    public String getCurrentEmail() { ... }
    public String getCurrentToken() { ... }  // Fuer API-Calls an PortalCore
}
```

### SPA Static Resource Filter

Wichtig fuer Angular-Routing: Wenn die App unter einem Sub-Pfad geladen wird, versucht der Browser relative Assets unter diesem Pfad zu laden. Der Filter leitet z.B. `/dokumente/main-HASH.js` nach `/main-HASH.js` um.

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaStaticResourceFilter extends OncePerRequestFilter {
    // Leitet /subpath/asset.js nach /asset.js um
}
```

### WebConfig fuer SPA-Routing

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/{path:[^\\.]*}").setViewName("forward:/index.html");
        registry.addViewController("/{path1}/{path2:[^\\.]*}").setViewName("forward:/index.html");
    }
}
```

---

## 5. Dashboard-Widget-Registrierung

Apps koennen Widgets fuer das Portal-Dashboard registrieren. Dies geschieht **beim App-Start** ueber einen REST-Call an PortalCore.

```java
@Service
public class WidgetRegistrationService {

    @Value("${portal.core.base-url:http://portal-backend:8080}")
    private String portalCoreBaseUrl;

    @Value("${portal.widget.registration-enabled:false}")
    private boolean registrationEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void registerWidgets() {
        if (!registrationEnabled) return;

        registerWidget("wb-neueste-dokumente", "Neueste Dokumente",
                "Zeigt die neuesten Dokumente an",
                "Wissensdatenbank", "/api/dokumente/neueste?limit=5",
                "/dokumente", 2, 2);

        registerWidget("wb-statistik", "Wissensdatenbank Statistik",
                "Zeigt Statistiken zur Wissensdatenbank",
                "Wissensdatenbank", "/api/dokumente/statistik",
                "/dokumente", 2, 1);
    }

    private void registerWidget(String widgetKey, String titel, String beschreibung,
                                 String kategorie, String datenEndpunkt,
                                 String linkZiel, int breite, int hoehe) {
        Map<String, Object> widget = Map.ofEntries(
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
    }
}
```

**Widget-Parameter:**
| Parameter | Beschreibung |
|-----------|-------------|
| `widgetKey` | Eindeutiger Schluessel (Prefix mit App-Kuerzel, z.B. `wb-`) |
| `widgetTyp` | `DATA_LIST` fuer Listen-Widgets |
| `appId` | Muss mit der App-ID im Portal uebereinstimmen |
| `datenEndpunkt` | Relativer API-Pfad, den das Portal aufruft um Daten zu holen |
| `linkZiel` | Route in der App, auf die das Widget verlinkt |
| `standardBreite/Hoehe` | Standardgroesse im 4-Spalten-Grid (1-4 / 1-4) |

---

## 6. Datenbank-Schema

### Konventionen
- **Tabellen-Prefix:** `wb_` (um Konflikte mit PortalCore und anderen Apps zu vermeiden)
- **Flyway-Tabelle:** `wb_schema_history` (eigene Migrations-Historie)
- **Tenant-Isolierung:** Jede Tabelle hat eine `tenant_id`-Spalte
- **IDs:** `VARCHAR(36)` (UUID-basiert)
- **Timestamps:** `TIMESTAMP NOT NULL DEFAULT NOW()`
- Shared Database: Die App nutzt die **gleiche** PostgreSQL-Datenbank wie PortalCore

### application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://portal-db:5432/portal}
    username: ${DB_USER:portal}
    password: ${DB_PASS:portal}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    table: wb_schema_history          # Eigene Migrations-Tabelle!
    validate-on-migrate: false

portal:
  jwt:
    secret: ${JWT_SECRET:ThisIsADevelopmentSecretKeyThatMustBeAtLeast32BytesLong!}
  core:
    base-url: ${PORTAL_CORE_BASE_URL:http://portal-backend:8080}

logging:
  level:
    de.wissensdatenbank: INFO
```

---

## 7. Dockerfile (Multi-Stage Build)

```dockerfile
# Stage 1: Build Angular Frontend
FROM node:18-alpine AS frontend-build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot Backend
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
# Angular-Build in Spring Boot static resources kopieren
COPY --from=frontend-build /app/dist/browser/ ./src/main/resources/static/
RUN mvn package -DskipTests -B

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

**Wichtig:** Das Angular-Frontend wird in die Spring-Boot-Static-Resources kopiert. Spring Boot liefert dann sowohl API als auch Frontend ueber Port 8080 aus.

---

## 8. Maven Dependencies (pom.xml)

Mindest-Dependencies fuer eine Portal-App:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.4</version>
</parent>

<properties>
    <java.version>21</java.version>
    <jjwt.version>0.12.5</jjwt.version>
</properties>

<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <!-- JPA + PostgreSQL -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>${jjwt.version}</version>
        <scope>runtime</scope>
    </dependency>
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 9. Frontend-Packages (package.json)

```json
{
  "dependencies": {
    "@angular/animations": "^18.0.0",
    "@angular/common": "^18.0.0",
    "@angular/compiler": "^18.0.0",
    "@angular/core": "^18.0.0",
    "@angular/forms": "^18.0.0",
    "@angular/platform-browser": "^18.0.0",
    "@angular/platform-browser-dynamic": "^18.0.0",
    "@angular/router": "^18.0.0",
    "rxjs": "~7.8.0",
    "tslib": "^2.6.0",
    "zone.js": "~0.14.0"
  },
  "devDependencies": {
    "@angular-devkit/build-angular": "^18.0.0",
    "@angular/cli": "^18.0.0",
    "@angular/compiler-cli": "^18.0.0",
    "typescript": "~5.4.0"
  }
}
```

---

## 10. Installations- und Deployment-Ablauf

### Wie eine App ins Portal kommt (Schritt fuer Schritt):

1. **Git-Repository bereitstellen** — Das Repository muss `portal-app.yaml` + `Dockerfile` im Root haben.

2. **Im Portal-AppStore installieren** — Admin gibt die Git-URL ein. PortalCore klont das Repo und liest `portal-app.yaml`.

3. **Manifest wird verarbeitet:**
   - `AppManifestReader` liest Name, Version, Port, Dockerfile-Pfad, Env-Vars
   - `pages` werden als JSON in `portal_apps.manifest_pages` gespeichert
   - `useCases` werden als `AppUseCase`-Eintraege gespeichert und ins RBAC-System integriert

4. **Build & Deploy** — PortalCore baut das Docker-Image aus dem Dockerfile und startet den Container im Docker-Netzwerk.

5. **App ist erreichbar** — Ueber den nginx Reverse Proxy wird die App unter einer internen URL verfuegbar gemacht. Diese URL wird als `applicationUrl` in der `installed_apps`-Tabelle gespeichert.

6. **Sidebar-Integration** — Die Pages aus dem Manifest erscheinen als Submenue unter dem App-Icon in der Portal-Sidebar. Jede Page verlinkt auf `/app/<appId>?page=<route>`.

7. **Widget-Registrierung** — Beim App-Start ruft `WidgetRegistrationService` den PortalCore-Endpunkt `/api/dashboard/widgets/register` auf und meldet Dashboard-Widgets an.

---

## 11. Checkliste fuer eine neue Portal-App

- [ ] `portal-app.yaml` im Root mit name, version, port, dockerfile, healthCheck, pages, useCases
- [ ] `Dockerfile` mit Multi-Stage Build (Angular → Spring Boot → Runtime)
- [ ] Backend: `JwtAuthenticationFilter` mit shared JWT-Secret
- [ ] Backend: `SecurityConfig` mit `frameOptions().disable()` und CORS
- [ ] Backend: `SecurityHelper` fuer Tenant/User-Zugriff in Services
- [ ] Backend: `SpaStaticResourceFilter` fuer Angular-Asset-Routing
- [ ] Backend: `WebConfig` mit SPA-Forwarding fuer Angular-Routen
- [ ] Backend: `WidgetRegistrationService` fuer Dashboard-Widgets
- [ ] Backend: Flyway-Migrationen mit eigenem Tabellen-Prefix (`wb_`) und Schema-History-Tabelle
- [ ] Backend: `application.yml` mit DB-Verbindung, JWT-Secret, Portal-Core-URL
- [ ] Backend: Health-Endpunkt (`/api/health`)
- [ ] Frontend: `portalAuthInterceptor` liest JWT aus `localStorage.getItem('portal_token')`
- [ ] Frontend: `app.component.ts` erkennt iframe via `window.self !== window.top`
- [ ] Frontend: Page-Navigation via `?page=<route>` Query-Parameter
- [ ] Frontend: Eigene Sidebar nur im Standalone-Modus (nicht im iframe)
- [ ] Multi-Tenancy: Alle Tabellen haben `tenant_id`, alle Queries filtern danach

---

## 12. Haeufige Stolperfallen

1. **Frame-Options vergessen** → App laesst sich nicht im iframe laden. Loesung: `headers.frameOptions(fo -> fo.disable())`
2. **CORS-Probleme** → `allowCredentials(true)` und `allowedOriginPatterns(List.of("*"))` setzen
3. **Angular-Assets 404** → `SpaStaticResourceFilter` fehlt — `/subpath/main.js` wird nicht nach `/main.js` umgeleitet
4. **JWT nicht gefunden** → Interceptor muss `localStorage.getItem('portal_token')` lesen (nicht `sessionStorage`)
5. **Flyway-Konflikte** → Eigenen `table`-Namen in `application.yml` verwenden (z.B. `wb_schema_history`)
6. **Tabellen-Konflikte** → Immer App-spezifisches Prefix verwenden (`wb_` statt generischer Namen)
7. **Berechtigungen greifen nicht** → `useCases` in `portal-app.yaml` muessen korrekte Keys haben, die mit den `pages[].useCase`-Referenzen uebereinstimmen
