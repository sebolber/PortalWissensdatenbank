#!/usr/bin/env bash
set -euo pipefail

# ============================================================
# Importiert eine Handbuch-JSON-Datei in die Wissensdatenbank
#
# Verwendung:
#   ./tools/import_handbuch.sh <json-datei> [<base-url>] [<jwt-token>]
#
# Beispiel:
#   ./tools/import_handbuch.sh import-handbuch-aenderungsdialog.json
#   ./tools/import_handbuch.sh import-handbuch-aenderungsdialog.json http://localhost:8080 eyJ...
# ============================================================

JSON_FILE="${1:?Bitte JSON-Datei angeben}"
BASE_URL="${2:-http://localhost:8080}"
JWT_TOKEN="${3:-}"

if [ ! -f "$JSON_FILE" ]; then
  echo "Fehler: Datei nicht gefunden: $JSON_FILE"
  exit 1
fi

FILE_SIZE=$(wc -c < "$JSON_FILE" | tr -d ' ')
echo "Importiere: $JSON_FILE ($FILE_SIZE Bytes)"
echo "Ziel: $BASE_URL/api/handbuch-import"

# Auth-Header nur hinzufuegen wenn Token vorhanden
AUTH_HEADER=""
if [ -n "$JWT_TOKEN" ]; then
  AUTH_HEADER="-H \"Authorization: Bearer $JWT_TOKEN\""
fi

echo ""
echo "Sende Import-Request..."

RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST \
  "${BASE_URL}/api/handbuch-import" \
  -H "Content-Type: application/json" \
  ${JWT_TOKEN:+-H "Authorization: Bearer $JWT_TOKEN"} \
  -d "@${JSON_FILE}")

HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n -1)

echo ""
if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
  echo "Import erfolgreich (HTTP $HTTP_CODE):"
  echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
else
  echo "Import fehlgeschlagen (HTTP $HTTP_CODE):"
  echo "$BODY" | python3 -m json.tool 2>/dev/null || echo "$BODY"
  exit 1
fi
