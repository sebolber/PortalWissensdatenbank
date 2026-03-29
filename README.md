# PortalWissensdatenbank

> Zentrale Wissensdatenbank mit KI-gestuetzter Kodierempfehlung fuer das PortalCore-Oekosystem

![Java 21](https://img.shields.io/badge/Java-21-orange) ![Spring Boot 3.2.4](https://img.shields.io/badge/Spring%20Boot-3.2.4-green) ![Angular 18](https://img.shields.io/badge/Angular-18-red) ![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue) ![Docker](https://img.shields.io/badge/Docker-ready-blue)

## Inhaltsverzeichnis

- [Ueber das Projekt](#ueber-das-projekt)
- [Technologie-Stack](#technologie-stack)
- [Architektur](#architektur)
- [Projektstruktur](#projektstruktur)
- [Voraussetzungen](#voraussetzungen)
- [Installation und Setup](#installation-und-setup)
- [Konfiguration](#konfiguration)
- [API-Dokumentation](#api-dokumentation)
- [SEG4-Import](#seg4-import)
- [KI-Kodierempfehlung](#ki-kodierempfehlung)
- [Datenbankschema](#datenbankschema)
- [Berechtigungen](#berechtigungen)
- [Integration mit PortalCore](#integration-mit-portalcore)
- [Weiterfuehrende Dokumentation](#weiterfuehrende-dokumentation)

---

## Ueber das Projekt

Die PortalWissensdatenbank ist eine **Portal-App** fuer zentrale Wissensverwaltung und KI-gestuetzte medizinische Kodierempfehlungen. Sie wird als Docker-Container im PortalCore-Netzwerk betrieben.

### Hauptfunktionen

- **Dokumentenverwaltung** mit Versionierung, Kategorien, Tags und Bewertungen
- **Wissensartikel** fuer SEG4-Empfehlungen, Artikel und Leitlinien
- **KI-Kodierempfehlung** via RAG (Retrieval-Augmented Generation)
- **SEG4-PDF-Import** mit automatischer Extraktion von Empfehlungen
- **Volltextsuche** mit deutschem Stemming (PostgreSQL GIN-Index)
- **Dokument-Upload** fuer dateibasierte KI-Kodierung
- **Dashboard-Widgets** (neueste Dokumente, Statistik)
- **Multi-Tenancy** mit mandantenspezifischer Datenisolation

---

## Technologie-Stack

| Schicht | Technologie |
|---------|-------------|
| **Backend** | Spring Boot 3.2.4, Java 21, Spring Data JPA, Spring Security |
| **Frontend** | Angular 18, TypeScript 5.4, Standalone Components |
| **Datenbank** | PostgreSQL 16 (geteilt mit PortalCore, Praefix `wb_`) |
| **Migrationen** | Flyway (6 Migrationen, Tabelle `wb_schema_history`) |
| **PDF-Verarbeitung** | Apache PDFBox 3.0.1 |
| **Authentifizierung** | JWT (geteiltes Secret mit PortalCore) |
| **Build** | Maven 3.9 (Backend), Angular CLI 18 (Frontend) |
| **Deployment** | Docker (Multi-Stage Build) |

---

## Architektur

Die Anwendung folgt einer **Schichtenarchitektur**:

```
Frontend (Angular 18)
    ↕ REST API
Controller → Service → Repository → Entity
    ↕                    ↕
PortalCore API      PostgreSQL (wb_*)
(Auth, LLM, Rechte)
```

Detaillierte Dokumentation: [docs/arc42-architecture.md](docs/arc42-architecture.md)

---

## Projektstruktur

```
backend/
  src/main/java/de/wissensdatenbank/
    controller/       7 REST-Controller
    service/          9 Business-Services
    entity/           10 JPA-Entities
    repository/       9 Spring Data Repositories
    dto/              12 Data Transfer Objects
    enums/            4 Enumerationen
    config/           Security, JWT, CORS, SPA
    llm/              LLM-Integration (Client, PromptBuilder)
    parser/           SEG4-PDF-Parser (7 Klassen)
    retrieval/        Knowledge-Retrieval (5 Klassen)
    audit/            Audit-Service
  src/main/resources/
    db/migration/     6 Flyway-Migrationen (V1-V6)
    application.yml

frontend/
  src/app/
    pages/            9+ Feature-Komponenten
    services/         5 Angular Services
    models/           TypeScript Interfaces
    interceptors/     Auth-Interceptor

portal-app.yaml       App-Manifest fuer PortalCore
Dockerfile            Multi-Stage Build
```

---

## Voraussetzungen

- Java 21+
- Maven 3.9+
- Node.js 18+
- Angular CLI 18
- PostgreSQL 16
- Docker (fuer Deployment)
- Laufende PortalCore-Instanz (fuer Auth, LLM, Berechtigungen)

---

## Installation und Setup

### 1. Backend starten

```bash
cd backend
mvn spring-boot:run
```

API verfuegbar unter `http://localhost:8080/api`.

### 2. Frontend starten (Entwicklung)

```bash
cd frontend
npm install
ng serve
```

Frontend verfuegbar unter `http://localhost:4200`.

### 3. Docker Build & Run

```bash
docker build -t wissensdatenbank .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://portal-db:5432/portal \
  -e DB_USER=portal \
  -e DB_PASS=portal \
  -e JWT_SECRET=<shared-secret> \
  -e PORTAL_CORE_BASE_URL=http://portal-backend:8080 \
  wissensdatenbank
```

---

## Konfiguration

### Umgebungsvariablen

| Variable | Beschreibung | Standard |
|----------|-------------|---------|
| `DB_URL` | JDBC-URL der PostgreSQL-Datenbank | `jdbc:postgresql://portal-db:5432/portal` |
| `DB_USER` | Datenbank-Benutzer | `portal` |
| `DB_PASS` | Datenbank-Passwort | `portal` |
| `JWT_SECRET` | JWT-Signaturschluessel (geteilt mit PortalCore) | - |
| `PORTAL_CORE_BASE_URL` | URL des PortalCore-Backends | `http://portal-backend:8080` |
| `CORS_ALLOWED_ORIGINS` | Erlaubte CORS-Origins | `*` |
| `WIDGET_REGISTRATION_ENABLED` | Dashboard-Widgets registrieren | `false` |

---

## API-Dokumentation

Basis-URL: `http://<host>:8080/api`

### Dokumente (`/api/dokumente`)

| Methode | Pfad | Berechtigung | Beschreibung |
|---------|------|-------------|--------------|
| GET | `/dokumente` | lesen | Dokumente auflisten (paginiert, filterbar) |
| GET | `/dokumente/{id}` | lesen | Einzelnes Dokument abrufen |
| POST | `/dokumente` | schreiben | Dokument erstellen |
| PUT | `/dokumente/{id}` | schreiben | Dokument aktualisieren |
| PUT | `/dokumente/{id}/publish` | veroeffentlichen | Dokument publizieren |
| PUT | `/dokumente/{id}/archive` | veroeffentlichen | Dokument archivieren |
| DELETE | `/dokumente/{id}` | schreiben | Dokument loeschen |
| GET | `/dokumente/{id}/versionen` | lesen | Versionshistorie |
| POST | `/dokumente/{id}/feedback` | lesen | Bewertung abgeben |
| GET | `/dokumente/neueste` | lesen | Neueste Dokumente (Widget) |
| GET | `/dokumente/beliebt` | lesen | Meistgelesene Dokumente |

**Query-Parameter fuer GET /dokumente:**
- `status` - DRAFT, PUBLISHED, ARCHIVED
- `categoryId` - Kategorie-ID
- `q` - Suchbegriff (Volltext)
- `page`, `size`, `sortBy`, `sortDir` - Paginierung

### Wissensartikel (`/api/knowledge`)

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| GET | `/knowledge` | Wissensartikel auflisten (filterbar nach Typ) |
| GET | `/knowledge/{id}` | Einzelnen Wissensartikel abrufen |
| DELETE | `/knowledge/{id}` | Wissensartikel loeschen |

### KI-Kodierempfehlung (`/api/suggestions`)

| Methode | Pfad | Beschreibung |
|---------|------|-------------|
| POST | `/suggestions` | Kodierempfehlung generieren |
| GET | `/suggestions/llm-models` | Verfuegbare LLM-Modelle |

**Request-Body (POST /suggestions):**
```json
{
  "dokumentText": "Patientenakte...",
  "diagnosen": ["J18.9", "I10"],
  "massnahmen": ["Intubation"],
  "modelConfigId": "optional-model-id"
}
```

### Dokument-Upload (`/api/document-suggestions`)

| Methode | Pfad | Content-Type | Beschreibung |
|---------|------|-------------|--------------|
| POST | `/document-suggestions/upload` | multipart/form-data | Datei hochladen fuer KI-Kodierung |
| GET | `/document-suggestions` | - | Alle Upload-Ergebnisse auflisten |
| GET | `/document-suggestions/{id}` | - | Einzelnes Ergebnis abrufen |

### SEG4-Import (`/api/seg4`)

| Methode | Pfad | Content-Type | Beschreibung |
|---------|------|-------------|--------------|
| POST | `/seg4/import` | multipart/form-data | SEG4-PDF importieren |

### Kategorien (`/api/kategorien`)

| Methode | Pfad | Berechtigung | Beschreibung |
|---------|------|-------------|--------------|
| GET | `/kategorien` | lesen | Alle Kategorien |
| POST | `/kategorien` | admin | Kategorie erstellen |
| PUT | `/kategorien/{id}` | admin | Kategorie aktualisieren |
| DELETE | `/kategorien/{id}` | admin | Kategorie loeschen |

### Tags (`/api/tags`)

| Methode | Pfad | Berechtigung | Beschreibung |
|---------|------|-------------|--------------|
| GET | `/tags` | lesen | Alle Tags |
| POST | `/tags` | admin | Tag erstellen |
| DELETE | `/tags/{id}` | admin | Tag loeschen |

### Health Check (`/api/health`)

| Methode | Pfad | Auth | Beschreibung |
|---------|------|------|-------------|
| GET | `/health` | Nein | Health Check fuer Docker |

---

## SEG4-Import

SEG4-PDFs (Schlichtungsempfehlungen) werden automatisch geparst:

1. **PDF-Upload** ueber `/api/seg4/import` (multipart/form-data)
2. **Textextraktion** via Apache PDFBox
3. **Block-Erkennung** einzelner Empfehlungen im Rohtext
4. **Feld-Extraktion**: Empfehlungsnummer, Schlagworte, Problemerlaeuterung, Empfehlung, Entscheidung, Zusatzhinweis
5. **Normalisierung** und Bestimmung der Verbindlichkeitsstufe
6. **Persistierung** als KnowledgeItem mit verknuepften Seg4Recommendations

---

## KI-Kodierempfehlung

Die RAG-Pipeline (Retrieval-Augmented Generation):

1. **Eingabe**: Dokumenttext, Diagnosen, Massnahmen
2. **Knowledge Retrieval**: PostgreSQL-Volltext-Suche + SEG4-spezifische Suche
3. **Ranking**: Relevanz-Scoring der gefundenen Wissensartikel
4. **Prompt Building**: System-Prompt (Kodierexperte) + User-Prompt (Kontext + Wissen)
5. **LLM-Aufruf**: Ueber PortalCore-LLM-Proxy
6. **Audit**: Vollstaendiger Log (Eingabe, Prompt, Antwort, Quellen)
7. **Antwort**: Strukturierte Empfehlungen mit Quellenangaben

---

## Datenbankschema

10 Entitaeten mit `wb_`-Praefix in geteilter PostgreSQL-Instanz:

| Tabelle | Beschreibung |
|---------|-------------|
| `wb_documents` | Dokumente mit Versionierung und Bewertungen |
| `wb_document_versions` | Versionshistorie der Dokumente |
| `wb_categories` | Hierarchische Kategorien |
| `wb_tags` | Tags fuer Dokumente und Wissensartikel |
| `wb_feedback` | Bewertungen (1-5 Sterne) |
| `wb_knowledge_items` | Wissensartikel (SEG4, Artikel, Leitlinien) |
| `wb_knowledge_sub_articles` | Unterartikel eines Wissensartikels |
| `wb_seg4_recommendations` | Einzelne SEG4-Empfehlungen |
| `wb_document_suggestions` | KI-Kodierempfehlungen (mit Datei) |
| `wb_suggestion_audit_log` | Audit-Trail fuer LLM-Aufrufe |

Vollstaendiges ER-Diagramm: [docs/er-diagram.md](docs/er-diagram.md)

---

## Berechtigungen

| Use-Case | Beschreibung |
|----------|-------------|
| `wissensdatenbank-lesen` | Dokumente, Wissensartikel, Kategorien, Tags lesen |
| `wissensdatenbank-schreiben` | Dokumente erstellen und bearbeiten |
| `wissensdatenbank-veroeffentlichen` | Dokumente publizieren und archivieren |
| `wissensdatenbank-kodierempfehlung` | KI-gestuetzte Kodierempfehlungen nutzen |
| `wissensdatenbank-admin` | Kategorien, Tags, SEG4-Import, Einstellungen verwalten |

Berechtigungen werden ueber PortalCore geprueft (5-Minuten-Cache). SuperAdmins haben automatisch Zugriff auf alle Use-Cases.

---

## Integration mit PortalCore

| Integration | Endpunkt | Beschreibung |
|------------|----------|-------------|
| **Authentifizierung** | JWT-Token | Geteiltes Secret, Token via Header oder Cookie |
| **Berechtigungen** | `POST /api/permissions/check` | Use-Case-Pruefung mit Caching |
| **LLM-Proxy** | `POST /api/llm/chat` | KI-Aufrufe ueber PortalCore (kein direkter API-Key) |
| **LLM-Modelle** | `GET /api/llm/models` | Verfuegbare Modelle auflisten |
| **Dashboard-Widgets** | `POST /api/dashboard/widgets/register` | Widget-Registrierung bei App-Start |

### Dashboard-Widgets

| Widget | Groesse | Beschreibung |
|--------|---------|-------------|
| `wb-neueste-dokumente` | 2x2 | Zeigt die 5 neuesten Dokumente |
| `wb-statistik` | 2x1 | Zeigt Wissensbasis-Statistiken |

---

## Weiterfuehrende Dokumentation

| Datei | Beschreibung |
|-------|-------------|
| [docs/arc42-architecture.md](docs/arc42-architecture.md) | Arc42 Architekturdokumentation |
| [docs/er-diagram.md](docs/er-diagram.md) | Vollstaendiges ER-Diagramm (Mermaid) |
| [portal-app.yaml](portal-app.yaml) | App-Manifest fuer PortalCore |
