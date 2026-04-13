#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Wissensdatenbank - Build & Start Skript
# ============================================================
#
# Verwendung:
#   ./start.sh              Baut Frontend + Backend und startet die Anwendung
#   ./start.sh --backend    Baut und startet nur das Backend (ohne Frontend-Build)
#   ./start.sh --docker     Baut und startet via Docker
#   ./start.sh --clean      Loescht Build-Artefakte vor dem Build
#
# Voraussetzungen (lokal):
#   - Java 21+
#   - Node 18+ / npm
#   - PostgreSQL (oder DB_URL Umgebungsvariable)
#
# Umgebungsvariablen (optional):
#   DB_URL       JDBC-URL (default: jdbc:postgresql://localhost:5432/portal)
#   DB_USER      DB-Benutzer (default: portal)
#   DB_PASS      DB-Passwort (default: portal)
#   JWT_SECRET   JWT-Secret (default: dev-secret-do-not-use-in-production)
#   SERVER_PORT  Server-Port (default: 8080)
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
FRONTEND_DIR="$SCRIPT_DIR/frontend"

# Farben
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()   { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1" >&2; }

# --- Defaults ---
export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/portal}"
export DB_USER="${DB_USER:-portal}"
export DB_PASS="${DB_PASS:-portal}"
export JWT_SECRET="${JWT_SECRET:-dev-secret-do-not-use-in-production}"
export PORTAL_CORE_BASE_URL="${PORTAL_CORE_BASE_URL:-http://localhost:8080}"

MODE="full"
CLEAN=false

for arg in "$@"; do
  case "$arg" in
    --backend) MODE="backend" ;;
    --docker)  MODE="docker" ;;
    --clean)   CLEAN=true ;;
    --help|-h)
      head -25 "$0" | tail -20
      exit 0
      ;;
  esac
done

# ============================================================
# Docker-Modus
# ============================================================
if [ "$MODE" = "docker" ]; then
  log "Baue Docker-Image..."
  docker build -t wissensdatenbank:latest "$SCRIPT_DIR"
  log "Starte Container..."
  docker run --rm -it \
    -p "${SERVER_PORT:-8080}:8080" \
    -e DB_URL="$DB_URL" \
    -e DB_USER="$DB_USER" \
    -e DB_PASS="$DB_PASS" \
    -e JWT_SECRET="$JWT_SECRET" \
    -e PORTAL_CORE_BASE_URL="$PORTAL_CORE_BASE_URL" \
    --name wissensdatenbank \
    wissensdatenbank:latest
  exit 0
fi

# ============================================================
# Java pruefen
# ============================================================
if ! command -v java &>/dev/null; then
  error "Java nicht gefunden. Bitte Java 21+ installieren."
  exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | sed 's/.*"\([0-9]*\)\..*/\1/')
if [ "$JAVA_VERSION" -lt 21 ] 2>/dev/null; then
  warn "Java $JAVA_VERSION erkannt - Java 21+ wird empfohlen."
fi

# ============================================================
# Maven Wrapper einrichten (falls nicht vorhanden)
# ============================================================
ensure_mvnw() {
  if [ -f "$BACKEND_DIR/mvnw" ]; then
    return
  fi

  if command -v mvn &>/dev/null; then
    log "Erstelle Maven Wrapper via mvn..."
    (cd "$BACKEND_DIR" && mvn wrapper:wrapper -Dmaven=3.9.6 -q)
  else
    log "Maven nicht installiert - lade Maven herunter..."
    MAVEN_VERSION="3.9.9"
    MAVEN_HOME="$HOME/.m2/wrapper/dists/apache-maven-${MAVEN_VERSION}"

    if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
      log "Lade Maven $MAVEN_VERSION herunter..."
      mkdir -p "$MAVEN_HOME"
      MAVEN_URL="https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
      curl -fsSL "$MAVEN_URL" | tar xz -C "$MAVEN_HOME" --strip-components=1
      if [ ! -f "$MAVEN_HOME/bin/mvn" ]; then
        error "Maven-Download fehlgeschlagen. Bitte Maven manuell installieren: brew install maven"
        exit 1
      fi
      log "Maven $MAVEN_VERSION installiert nach $MAVEN_HOME"
    fi

    # mvnw Skript erstellen das auf die heruntergeladene Maven-Installation zeigt
    cat > "$BACKEND_DIR/mvnw" << MVNW_EOF
#!/bin/sh
exec "$MAVEN_HOME/bin/mvn" "\$@"
MVNW_EOF
    chmod +x "$BACKEND_DIR/mvnw"
  fi
}

# ============================================================
# Frontend bauen
# ============================================================
build_frontend() {
  if ! command -v node &>/dev/null || ! command -v npm &>/dev/null; then
    warn "Node/npm nicht gefunden - ueberspringe Frontend-Build."
    warn "Installiere Node 18+ fuer den Frontend-Build."
    return 1
  fi

  log "Installiere Frontend-Abhaengigkeiten..."
  (cd "$FRONTEND_DIR" && npm install --silent)

  log "Baue Angular-Frontend..."
  (cd "$FRONTEND_DIR" && npx ng build --configuration production)

  # Angular-Build in Spring Boot static resources kopieren
  local DIST_DIR="$FRONTEND_DIR/dist/browser"
  local STATIC_DIR="$BACKEND_DIR/src/main/resources/static"

  if [ -d "$DIST_DIR" ]; then
    rm -rf "$STATIC_DIR"
    mkdir -p "$STATIC_DIR"
    cp -r "$DIST_DIR/"* "$STATIC_DIR/"
    log "Frontend-Build nach $STATIC_DIR kopiert."
  else
    warn "Frontend-Dist nicht gefunden unter $DIST_DIR"
    return 1
  fi
}

# ============================================================
# Backend bauen
# ============================================================
build_backend() {
  ensure_mvnw >&2

  if [ "$CLEAN" = true ]; then
    log "Bereinige Build-Artefakte..." >&2
    (cd "$BACKEND_DIR" && ./mvnw clean -q)
  fi

  log "Baue Spring Boot Backend..." >&2
  (cd "$BACKEND_DIR" && ./mvnw package -DskipTests -q)

  local jar
  jar=$(find "$BACKEND_DIR/target" -maxdepth 1 -name "*.jar" -not -name "*-sources*" | head -1)
  if [ -z "$jar" ]; then
    error "Kein JAR-File gefunden nach dem Build."
    exit 1
  fi
  echo "$jar"
}

# ============================================================
# Anwendung starten
# ============================================================
start_app() {
  local jar="$1"
  log "Starte Wissensdatenbank..."
  log "  DB_URL:  $DB_URL"
  log "  Port:    ${SERVER_PORT:-8080}"
  echo ""
  log "Anwendung erreichbar unter: http://localhost:${SERVER_PORT:-8080}"
  echo ""

  java -jar "$jar" \
    --server.port="${SERVER_PORT:-8080}" \
    --spring.datasource.url="$DB_URL" \
    --spring.datasource.username="$DB_USER" \
    --spring.datasource.password="$DB_PASS" \
    --portal.jwt.secret="$JWT_SECRET" \
    --portal.core.base-url="$PORTAL_CORE_BASE_URL"
}

# ============================================================
# Hauptprogramm
# ============================================================
log "Wissensdatenbank Build & Start"
echo "============================================"

if [ "$MODE" = "full" ]; then
  build_frontend || warn "Frontend-Build fehlgeschlagen - starte nur Backend."
fi

JAR_FILE=$(build_backend)
start_app "$JAR_FILE"
