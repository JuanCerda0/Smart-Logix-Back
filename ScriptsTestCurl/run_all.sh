#!/bin/bash
# =============================================================
# run_all.sh — Ejecuta todas las pruebas de integración de
#   SmartLogix en orden y muestra un resumen final.
#
#   Requisito previo:
#     docker compose up --build
#   (BFF en :8080, Inventory en :8081, Auth en :8082)
#
#   Uso:
#     chmod +x *.sh
#     ./run_all.sh
# =============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_common.sh"

reset_results

echo -e "${BOLD}"
echo "  ____                       _     _                _      "
echo " / ___| _ __ ___   __ _ _ __| |_  | |    ___   __ _(_)_  __ "
echo " \___ \| '_ \` _ \ / _\` | '__| __| | |   / _ \ / _\` | \ \/ / "
echo "  ___) | | | | | | (_| | |  | |_  | |__| (_) | (_| | |>  <  "
echo " |____/|_| |_| |_|\__,_|_|   \__| |_____\___/ \__, |_/_/\_\ "
echo "                                               |___/        "
echo -e "${NC}"
echo "Suite de pruebas de integración — verifica comunicación real entre servicios"
echo ""

# --- Verifica que los 3 servicios respondan antes de empezar ---
section "PRE-CHECK — Verificando que los servicios estén levantados"

check_service() {
    local name="$1"
    local url="$2"
    if curl -s -o /dev/null -w "%{http_code}" --max-time 3 "$url" | grep -qE "^[0-9]+$"; then
        echo -e "  ${GREEN}✔${NC} $name responde en $url"
        return 0
    else
        echo -e "  ${RED}✘${NC} $name NO responde en $url"
        return 1
    fi
}

ALL_UP=true
check_service "Auth Service     " "$AUTH_URL/swagger-ui.html" || ALL_UP=false
check_service "Inventory Service" "$INVENTORY_URL/actuator/health" || ALL_UP=false
check_service "BFF              " "$BFF_URL/swagger-ui.html" || ALL_UP=false

if [ "$ALL_UP" = false ]; then
    echo ""
    echo -e "${RED}${BOLD}Uno o más servicios no responden.${NC}"
    echo -e "${YELLOW}Verifica que 'docker compose up --build' esté corriendo y espera unos segundos más.${NC}"
    exit 1
fi

# --- Ejecuta los 3 bloques de pruebas en orden ---
bash "$SCRIPT_DIR/01_auth_service.sh"
bash "$SCRIPT_DIR/02_bff_flow.sh"
bash "$SCRIPT_DIR/03_inventory_direct.sh"

# --- Resumen final ---
section "RESUMEN FINAL"

TOTAL=$(wc -l < "$RESULTS_FILE")
PASS_COUNT=$(grep -c "|PASS|" "$RESULTS_FILE")
FAIL_COUNT=$(grep -c "|FAIL" "$RESULTS_FILE")
INFO_COUNT=$(grep -c "|INFO" "$RESULTS_FILE")
HALLAZGO_COUNT=$(grep -c "|HALLAZGO" "$RESULTS_FILE")

echo ""
printf "  %-55s %s\n" "TEST" "RESULTADO"
echo "  ─────────────────────────────────────────────────────────────────"
while IFS='|' read -r name status detail; do
    case "$status" in
        PASS) color="$GREEN" ;;
        FAIL*) color="$RED" ;;
        HALLAZGO) color="$YELLOW" ;;
        *) color="$YELLOW" ;;
    esac
    printf "  %-55s ${color}%s${NC} (%s)\n" "$name" "$status" "$detail"
done < "$RESULTS_FILE"

echo ""
echo -e "  ${BOLD}Total: $TOTAL   ${GREEN}PASS: $PASS_COUNT${NC}   ${RED}FAIL: $FAIL_COUNT${NC}   ${YELLOW}INFO: $INFO_COUNT${NC}   ${YELLOW}HALLAZGOS: $HALLAZGO_COUNT${NC}"
echo ""

if [ "$FAIL_COUNT" -gt 0 ]; then
    echo -e "${RED}Hay $FAIL_COUNT prueba(s) fallida(s). Revisa el detalle arriba.${NC}"
    exit 1
else
    echo -e "${GREEN}Todas las pruebas críticas pasaron correctamente.${NC}"
    exit 0
fi
