# PortalWissensdatenbank - Arc42 Architekturdokumentation

> Version 1.0.0 | Stand: 2026-03-29

## Inhaltsverzeichnis

1. [Einfuehrung und Ziele](#1-einfuehrung-und-ziele)
2. [Randbedingungen](#2-randbedingungen)
3. [Kontextabgrenzung](#3-kontextabgrenzung)
4. [Loesungsstrategie](#4-loesungsstrategie)
5. [Bausteinsicht](#5-bausteinsicht)
6. [Laufzeitsicht](#6-laufzeitsicht)
7. [Verteilungssicht](#7-verteilungssicht)
8. [Querschnittliche Konzepte](#8-querschnittliche-konzepte)
9. [Architekturentscheidungen](#9-architekturentscheidungen)
10. [Qualitaetsanforderungen](#10-qualitaetsanforderungen)
11. [Risiken und technische Schulden](#11-risiken-und-technische-schulden)
12. [Glossar](#12-glossar)

---

## 1. Einfuehrung und Ziele

### 1.1 Aufgabenstellung

Die PortalWissensdatenbank ist eine **Portal-App** innerhalb des PortalCore-Oekosystems. Sie bietet:

- **Zentrale Wissensdatenbank** fuer medizinische Kodierempfehlungen
- **KI-gestuetzte Kodierempfehlungen** basierend auf hochgeladenen Dokumenten
- **SEG4-PDF-Import** mit automatischer Extraktion von Empfehlungen
- **Dokumentenverwaltung** mit Versionierung, Kategorien und Tags
- **Volltextsuche** mit deutschem Stemming

### 1.2 Qualitaetsziele

| Prioritaet | Qualitaetsziel | Beschreibung |
|------------|----------------|--------------|
| 1 | **Genauigkeit** | KI-Empfehlungen basieren auf verifiziertem Wissen (RAG-Muster) |
| 2 | **Nachvollziehbarkeit** | Vollstaendiger Audit-Trail fuer LLM-Aufrufe (Compliance) |
| 3 | **Suchqualitaet** | Deutsches Stemming und Volltext-Indizes fuer praezise Ergebnisse |
| 4 | **Integration** | Nahtlose Einbettung in PortalCore (JWT, Widgets, Berechtigungen) |

### 1.3 Stakeholder

| Rolle | Erwartung |
|-------|-----------|
| **Kodierfachkraefte** | Schnelle, zuverlaessige Kodierempfehlungen aus der Wissensbasis |
| **Medizincontroller** | Qualitaetsgesicherte SEG4-Empfehlungen, nachvollziehbare KI-Entscheidungen |
| **Administratoren** | Kategorien-, Tag- und Dokumentenverwaltung, SEG4-Import |
| **Betrieb** | Zuverlaessiger Betrieb als Docker-Container im PortalCore-Netzwerk |

---

## 2. Randbedingungen

### 2.1 Technische Randbedingungen

| Randbedingung | Beschreibung |
|---------------|--------------|
| Portal-App | Laeuft als eigenstaendiger Container im PortalCore-Oekosystem |
| Geteilte Datenbank | PostgreSQL-Instanz geteilt mit PortalCore, Tabellen-Praefix `wb_` |
| JWT-Authentifizierung | Token von PortalCore, geteiltes Secret |
| LLM-Proxy | LLM-Aufrufe ueber PortalCore-API (kein direkter API-Key) |
| Java 21, Spring Boot 3.2.4 | Backend-Framework |
| Angular 18 | Frontend-Framework |
| Apache PDFBox 3.0.1 | PDF-Verarbeitung fuer SEG4-Import |

### 2.2 Konventionen

- Flyway-Migrationen mit Praefix `wb_` und eigener Historientabelle `wb_schema_history`
- REST-API unter `/api/`
- Multi-Tenancy via `tenant_id` in allen Tabellen
- Berechtigungspruefung via PortalCore-API mit 5-Minuten-Cache

---

## 3. Kontextabgrenzung

### 3.1 Fachlicher Kontext

```
  Kodierfachkraft                Administrator
       |                              |
       v                              v
  +------------------------------------------+
  |        PortalWissensdatenbank             |
  |                                          |
  |  - Dokumente durchsuchen                 |
  |  - KI-Kodierempfehlung anfordern         |
  |  - Wissensartikel einsehen               |
  |  - Dokumente bewerten                    |
  |  - SEG4-PDFs importieren                 |
  |  - Kategorien/Tags verwalten             |
  +------------------+-----------------------+
                     |
        +------------+------------+
        |                         |
   +----v------+          +------v-------+
   | PortalCore|          | PostgreSQL   |
   | - Auth    |          | (geteilte DB)|
   | - LLM     |          +--------------+
   | - Rechte  |
   | - Widgets |
   +-----------+
```

### 3.2 Technischer Kontext

| Schnittstelle | Richtung | Technologie | Beschreibung |
|---------------|----------|-------------|--------------|
| PortalCore Auth | eingehend | JWT | Token-Validierung via geteiltes Secret |
| PortalCore Permissions | ausgehend | REST | Berechtigungspruefung `/api/permissions/check` |
| PortalCore LLM | ausgehend | REST | LLM-Chat-Proxy `/api/llm/chat` |
| PortalCore Dashboard | ausgehend | REST | Widget-Registrierung `/api/dashboard/widgets/register` |
| Browser | eingehend | HTTPS/iframe | Angular SPA, eingebettet in PortalCore |
| PostgreSQL | ausgehend | JDBC | Datenbankzugriff (Tabellen mit `wb_`-Praefix) |

---

## 4. Loesungsstrategie

| Entscheidung | Begruendung |
|--------------|-------------|
| **RAG-Muster** | Retrieval-Augmented Generation: Wissen aus DB abrufen, als Kontext an LLM uebergeben → gruendlichere, quellenbasierte Empfehlungen |
| **SEG4-Parser-Pipeline** | Mehrstufiger PDF-Parser (Extraktion → Block-Erkennung → Feld-Extraktion → Normalisierung) fuer zuverlaessigen Import |
| **Volltext-Suche (Deutsch)** | PostgreSQL GIN-Indizes mit deutschem Stemming fuer praezise Suche |
| **Dokument-Versionierung** | Automatische Versionshistorie bei jeder Aenderung |
| **Permission-Caching** | 5-Minuten-Cache fuer PortalCore-Berechtigungspruefungen |
| **Shared Database** | Gleiche PostgreSQL-Instanz wie PortalCore, isoliert durch Tabellen-Praefix |

---

## 5. Bausteinsicht

### 5.1 Ebene 1 - Gesamtsystem

```
+------------------------------------------------------------------+
|                    PortalWissensdatenbank                          |
|                                                                    |
|  +-------------------+    REST API    +-------------------------+  |
|  |    Frontend       |<-------------->|      Backend            |  |
|  |    (Angular 18)   |                |   (Spring Boot 3.2)     |  |
|  +-------------------+                +----------+--------------+  |
|                                                  |                 |
|                                       +----------v--------------+  |
|                                       |    PostgreSQL (wb_*)    |  |
|                                       +-------------------------+  |
+------------------------------------------------------------------+
                    |
              +-----v------+
              |  PortalCore |
              |  (Auth/LLM) |
              +-------------+
```

### 5.2 Ebene 2 - Backend-Komponenten

| Komponente | Controller | Services | Beschreibung |
|------------|-----------|----------|--------------|
| **Dokumente** | DocumentController | DocumentService | CRUD, Versionierung, Bewertungen, Statistik |
| **Wissen** | KnowledgeController | KnowledgeService | Wissensartikel (SEG4, Artikel, Leitlinien) |
| **Kategorien** | CategoryController | CategoryService | Hierarchische Kategorien |
| **Tags** | TagController | TagService | Tag-Verwaltung |
| **KI-Empfehlung** | SuggestionController | SuggestionService, PromptBuilder, KnowledgeSearchService | RAG-Pipeline fuer Kodierempfehlungen |
| **Dokument-Upload** | DocumentSuggestionController | DocumentSuggestionService | Datei-Upload fuer KI-Kodierung |
| **SEG4-Import** | Seg4ImportController | Seg4ImportService, Seg4PdfParser, Seg4BlockDetector, Seg4FieldExtractor, Seg4Normalizer | PDF-Import-Pipeline |
| **Sicherheit** | - | PermissionService, SecurityHelper | Berechtigungspruefung via PortalCore |
| **Audit** | - | AuditService | LLM-Audit-Trail |
| **Widgets** | - | WidgetRegistrationService | Dashboard-Widget-Registrierung |

### 5.3 Ebene 2 - Frontend-Seiten

| Route | Komponente | Berechtigung | Beschreibung |
|-------|-----------|-------------|--------------|
| `/` | StartseiteComponent | - | Startseite/Dashboard |
| `/dokumente` | DokumentListComponent | lesen | Dokumentenliste |
| `/dokumente/:id` | DokumentDetailComponent | lesen | Dokumentdetail mit Bewertung |
| `/dokumente/neu` | DokumentFormComponent | schreiben | Dokument erstellen/bearbeiten |
| `/wissen` | KnowledgeListComponent | lesen | Wissensartikel-Browser |
| `/wissen/:id` | KnowledgeDetailComponent | lesen | Wissensartikel-Detail |
| `/kodierempfehlung` | ChatComponent | kodierempfehlung | KI-Empfehlung (Text-Eingabe) |
| `/dokument-kodierung` | DokumentKodierungComponent | kodierempfehlung | KI-Empfehlung (Datei-Upload) |
| `/seg4-import` | Seg4ImportComponent | admin | SEG4-PDF-Import |
| `/kategorien` | KategorienComponent | admin | Kategorienverwaltung |
| `/suche` | SucheComponent | lesen | Volltextsuche |
| `/konfiguration` | KonfigurationComponent | admin | Einstellungen |

---

## 6. Laufzeitsicht

### 6.1 KI-Kodierempfehlung generieren

```
Benutzer        Frontend        SuggestionCtrl   SuggestionSvc   KnowledgeSearch   PromptBuilder   LlmClient      AuditSvc
   |               |                |                |                |                |              |              |
   |-- Text+Diag ->|                |                |                |                |              |              |
   |               |-- POST ------->|                |                |                |              |              |
   |               |  /suggestions  |-- generate --->|                |                |              |              |
   |               |                |                |-- search ----->|                |              |              |
   |               |                |                |                |-- Fulltext --->DB              |              |
   |               |                |                |                |-- SEG4-Match ->DB              |              |
   |               |                |                |                |-- Ranking ----->|              |              |
   |               |                |                |<-- Candidates -|                |              |              |
   |               |                |                |-- buildPrompt -|--------------->|              |              |
   |               |                |                |                |                |              |              |
   |               |                |                |-- chat --------|----------------|------------->|              |
   |               |                |                |                |                |  POST        |              |
   |               |                |                |                |                |  /api/llm/   |              |
   |               |                |                |                |                |  chat        |              |
   |               |                |                |<-- Response ---|----------------|--------------|              |
   |               |                |                |-- log ---------|----------------|--------------|------------>|
   |               |                |<-- Response ---|                |                |              |              |
   |               |<-- 200 --------|                |                |                |              |              |
   |<-- Empfehlung |                |                |                |                |              |              |
```

### 6.2 SEG4-PDF-Import

```
Admin           Frontend        Seg4ImportCtrl   Seg4ImportSvc   PdfParser   BlockDetector   FieldExtractor   Normalizer
  |                |                |                |              |              |               |              |
  |-- PDF-Upload ->|                |                |              |              |               |              |
  |                |-- POST ------->|                |              |              |               |              |
  |                |  /seg4/import  |-- import ----->|              |              |               |              |
  |                |                |                |-- parse ---->|              |               |              |
  |                |                |                |<-- rawText --|              |               |              |
  |                |                |                |-- detect ----|------------->|               |              |
  |                |                |                |<-- blocks ---|--------------|               |              |
  |                |                |                |-- extract ---|--------------|-------------->|              |
  |                |                |                |<-- fields ---|--------------|---------------|              |
  |                |                |                |-- normalize -|--------------|---------------|------------>|
  |                |                |                |<-- clean ----|--------------|---------------|-------------|
  |                |                |                |-- persist -->DB             |               |              |
  |                |                |<-- Result -----|              |              |               |              |
  |                |<-- 200 --------|                |              |              |               |              |
  |<-- Ergebnis ---|                |                |              |              |               |              |
```

### 6.3 Dokument-Lebenszyklus

```
  DRAFT ──────> PUBLISHED ──────> ARCHIVED
    │               │
    │  publish()    │  archive()
    │               │
    └── update() ───┘
         (neue Version)
```

---

## 7. Verteilungssicht

```
+---------------------------------------------------------------+
|                    Docker Host                                 |
|                                                                |
|  +------------------+    +------------------------+            |
|  | PortalCore       |    | Wissensdatenbank       |            |
|  | Backend :8080    |    | (Angular + Spring Boot)|            |
|  |                  |<---|  :8081                  |            |
|  | - Auth/JWT       |    |  - /api/dokumente      |            |
|  | - LLM-Proxy      |    |  - /api/knowledge      |            |
|  | - Permissions     |    |  - /api/suggestions    |            |
|  | - Widgets         |    |  - /api/seg4           |            |
|  +--------+---------+    +----------+-------------+            |
|           |                         |                          |
|           +------------+------------+                          |
|                        |                                       |
|              +---------v-----------+                           |
|              |   PostgreSQL 16     |                           |
|              |   :5432             |                           |
|              |                     |                           |
|              |   PortalCore-       |                           |
|              |   Tabellen          |                           |
|              |   +                 |                           |
|              |   wb_*-Tabellen     |                           |
|              +---------------------+                           |
+---------------------------------------------------------------+
```

**Multi-Stage Docker Build:**
1. Stage 1: Node 18 → Angular Frontend bauen
2. Stage 2: Maven → Frontend in static/ kopieren, Spring Boot JAR bauen
3. Stage 3: Eclipse Temurin 21 JRE → JAR ausfuehren (Port 8080)

---

## 8. Querschnittliche Konzepte

### 8.1 Multi-Tenancy
- Alle Tabellen enthalten `tenant_id`
- SecurityHelper extrahiert Mandant aus JWT-Claims
- Alle Queries filtern nach aktuellem Mandanten

### 8.2 Berechtigungssystem

| Use-Case | Beschreibung |
|----------|-------------|
| `wissensdatenbank-lesen` | Dokumente und Wissen lesen |
| `wissensdatenbank-schreiben` | Dokumente erstellen und bearbeiten |
| `wissensdatenbank-veroeffentlichen` | Dokumente publizieren und archivieren |
| `wissensdatenbank-kodierempfehlung` | KI-Kodierempfehlungen nutzen |
| `wissensdatenbank-admin` | Administration (Kategorien, Tags, SEG4-Import) |

### 8.3 LLM-Audit-Trail
- Jeder LLM-Aufruf wird in `wb_suggestion_audit_log` gespeichert
- Gespeichert werden: Eingabetext, System-Prompt, User-Prompt, LLM-Antwort, verwendete Quellen
- Dient der Nachvollziehbarkeit und Compliance

### 8.4 Knowledge Retrieval (RAG)
1. Suchbegriffe aus Diagnosen und Massnahmen extrahieren
2. PostgreSQL-Volltext-Suche (deutsches Stemming)
3. SEG4-spezifische Suche ueber Empfehlungsnummern
4. Relevanz-Scoring und Ranking
5. Top-Ergebnisse als Kontext in LLM-Prompt

---

## 9. Architekturentscheidungen

### ADR-1: RAG statt Fine-Tuning

**Kontext:** KI-Empfehlungen sollen auf aktuellem Wissen basieren.

**Entscheidung:** Retrieval-Augmented Generation (RAG) statt LLM-Fine-Tuning.

**Begruendung:**
- Wissen aendert sich (neue SEG4-Empfehlungen) → kein Re-Training noetig
- Quellen sind nachvollziehbar (Audit-Trail)
- Geringere Kosten als Fine-Tuning
- Keine eigene GPU-Infrastruktur noetig

### ADR-2: Geteilte Datenbank mit Tabellen-Praefix

**Kontext:** Portal-Apps benoetigen Datenbankzugriff.

**Entscheidung:** Gleiche PostgreSQL-Instanz wie PortalCore, Tabellen mit `wb_`-Praefix.

**Begruendung:**
- Einfache Infrastruktur (eine DB-Instanz)
- Flyway-Isolation ueber eigene Historientabelle `wb_schema_history`
- Mandanten-Isolation bleibt erhalten durch `tenant_id`

### ADR-3: Apache PDFBox fuer SEG4-Import

**Kontext:** SEG4-Empfehlungen liegen als PDF vor.

**Entscheidung:** Apache PDFBox 3.0.1 fuer PDF-Textextraktion.

**Begruendung:**
- Bewaeaehrte Java-Bibliothek fuer PDF-Verarbeitung
- Open Source, keine Lizenzkosten
- Mehrstufige Parser-Pipeline fuer strukturierte Extraktion

---

## 10. Qualitaetsanforderungen

| ID | Szenario | Qualitaetsmerkmal |
|----|----------|-------------------|
| QS-1 | KI-Empfehlung basiert auf aktuellen SEG4-Daten, nicht auf veraltetem Wissen | Genauigkeit |
| QS-2 | Jede LLM-Abfrage ist mit Quellen und Prompt im Audit-Log nachvollziehbar | Nachvollziehbarkeit |
| QS-3 | Suche findet Dokumente auch bei Wortvarianten (deutsches Stemming) | Suchqualitaet |
| QS-4 | Aenderungen an Dokumenten erzeugen automatisch eine neue Version | Zuverlaessigkeit |
| QS-5 | Berechtigungen werden korrekt von PortalCore ueberprueft | Sicherheit |

---

## 11. Risiken und technische Schulden

| Risiko | Beschreibung | Massnahme |
|--------|--------------|-----------|
| **LLM-Abhaengigkeit** | KI-Empfehlungen haengen von PortalCore-LLM-Proxy ab | Graceful Degradation, Fehlerbehandlung |
| **PDF-Parsing-Qualitaet** | SEG4-PDFs koennen variieren, Parser muss robust sein | Umfangreiche Tests fuer Parser-Pipeline |
| **BYTEA-Speicherung** | Hochgeladene Dateien direkt in der DB | Objektspeicher fuer groessere Dateien evaluieren |
| **Permission-Cache** | 5-Minuten-Cache kann bei Rechteentzug zu verzoegertem Effekt fuehren | Cache-TTL konfigurierbar machen |

---

## 12. Glossar

| Begriff | Beschreibung |
|---------|--------------|
| **SEG4** | Schlichtungsempfehlungen der Schlichtungsstellen fuer medizinische Kodierung |
| **RAG** | Retrieval-Augmented Generation - Wissen aus DB als Kontext fuer LLM |
| **Kodierempfehlung** | KI-generierte Empfehlung zur medizinischen Kodierung |
| **Binding Level** | Verbindlichkeitsstufe: VERBINDLICH, EMPFEHLUNG, LEX_SPECIALIS, INFORMATIV |
| **Knowledge Type** | Wissenstyp: SEG4, ARTICLE, GUIDELINE |
| **Portal-App** | Anwendung, die im PortalCore-Oekosystem deployt wird |
| **Mandant (Tenant)** | Isolierte Organisationseinheit mit eigenen Daten |
| **Chunk** | Textabschnitt eines Dokuments fuer RAG-Retrieval |
| **Stemming** | Linguistische Reduktion auf Wortstamm (z.B. "Behandlung" → "behandl") |
