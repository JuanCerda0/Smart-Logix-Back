#!/bin/bash
# =============================================================
# _common.sh — Variables y helpers compartidos por los scripts
#   de prueba de SmartLogix. No se ejecuta solo, es importado
#   por los demás scripts con: source ./_common.sh
# =============================================================

# --- URLs base (servicios levantados con docker compose up --build) ---
BFF_URL="http://localhost:8080"
AUTH_URL="http://localhost:8082"
INVENTORY_URL="http://localhost:8081"

# --- Tenants de prueba (ya vienen seedeados según los README) ---
TENANT_A="empresa1"
TENANT_B="empresa2"
USER_A="admin"
PASS_A="admin123"
USER_B="admin"
PASS_B="admin123"

# --- Colores para que la salida sea legible ---
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# --- Contadores globales de resultados (persisten via archivo temporal) ---
RESULTS_FILE="/tmp/smartlogix_test_results.txt"

reset_results() {
    : > "$RESULTS_FILE"
}

record_result() {
    # record_result "nombre del test" "PASS|FAIL" "detalle opcional"
    echo "$1|$2|$3" >> "$RESULTS_FILE"
}

# --- Helper: imprime un encabezado de sección ---
section() {
    echo ""
    echo -e "${BLUE}${BOLD}═══════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}${BOLD}  $1${NC}"
    echo -e "${BLUE}${BOLD}═══════════════════════════════════════════════════${NC}"
}

# --- Helper: imprime un paso individual ---
step() {
    echo -e "${YELLOW}▶ $1${NC}"
}

# --- Helper: valida código HTTP esperado y registra el resultado ---
# uso: assert_status "<nombre test>" "<status_obtenido>" "<status_esperado>"
assert_status() {
    local name="$1"
    local got="$2"
    local expected="$3"

    if [ "$got" == "$expected" ]; then
        echo -e "  ${GREEN}✔ PASS${NC} — $name (HTTP $got)"
        record_result "$name" "PASS" "HTTP $got"
    else
        echo -e "  ${RED}✘ FAIL${NC} — $name (esperado HTTP $expected, obtuvo HTTP $got)"
        record_result "$name" "FAIL" "esperado $expected, obtuvo $got"
    fi
}

# --- Helper: ejecuta curl y separa body / status code ---
# uso: do_curl <metodo> <url> [headers...] -- [body_json]
# Devuelve en variables globales: HTTP_BODY y HTTP_STATUS
do_curl() {
    local response
    response=$(curl -s -w "\n%{http_code}" "$@")
    HTTP_STATUS=$(echo "$response" | tail -n1)
    HTTP_BODY=$(echo "$response" | sed '$d')
}

# --- Helper: extrae un campo simple de un JSON plano (sin jq obligatorio) ---
extract_json_field() {
    # extract_json_field '{"token":"abc"}' token  -> abc
    local json="$1"
    local field="$2"
    echo "$json" | grep -o "\"$field\"[[:space:]]*:[[:space:]]*\"[^\"]*\"" | head -1 | sed -E "s/.*:\s*\"([^\"]*)\"/\1/"
}

check_dependencies() {
    if ! command -v curl &> /dev/null; then
        echo -e "${RED}Error: curl no está instalado.${NC}"
        exit 1
    fi
}

check_dependencies
