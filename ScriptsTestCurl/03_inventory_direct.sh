#!/bin/bash
# =============================================================
# 03_inventory_direct.sh — Pruebas directas a Inventory (8081),
#   SALTÁNDOSE el BFF.
#
#   Confirmado en el código de TenantFilter: el filtro SOLO lee
#   el header X-Tenant-Id y nunca decodifica ni compara el claim
#   "tenant" del JWT. Por lo tanto el test [CRÍTICO] de abajo
#   debería devolver HTTP 200 (no 401/403) cuando el JWT y el
#   header no coinciden — eso confirma que el aislamiento de
#   tenant vía JWT vive únicamente en el BFF (TenantAccessValidator),
#   y que Inventory, llamado directo, confía ciegamente en el header.
# =============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_common.sh"

section "INVENTORY SERVICE — http://localhost:8081 (acceso DIRECTO, sin pasar por el BFF)"

# Reutiliza tokens generados por 01_auth_service.sh o 02_bff_flow.sh si existen
if [ -f /tmp/smartlogix_token_empresa1.txt ]; then
    TOKEN_A=$(cat /tmp/smartlogix_token_empresa1.txt)
fi
if [ -f /tmp/smartlogix_token_empresa2.txt ]; then
    TOKEN_B=$(cat /tmp/smartlogix_token_empresa2.txt)
fi

if [ -z "$TOKEN_A" ] || [ -z "$TOKEN_B" ]; then
    echo -e "${YELLOW}No se encontraron tokens guardados. Generándolos ahora vía Auth Service...${NC}"
    do_curl -X POST "$AUTH_URL/auth/login" -H "X-Tenant-Id: $TENANT_A" -H "Content-Type: application/json" -d "{\"username\":\"$USER_A\",\"password\":\"$PASS_A\"}"
    TOKEN_A=$(extract_json_field "$HTTP_BODY" "token")
    do_curl -X POST "$AUTH_URL/auth/login" -H "X-Tenant-Id: $TENANT_B" -H "Content-Type: application/json" -d "{\"username\":\"$USER_B\",\"password\":\"$PASS_B\"}"
    TOKEN_B=$(extract_json_field "$HTTP_BODY" "token")
fi

# --- 1. Caso normal: header y JWT coinciden (empresa1 / empresa1) ---
step "Caso normal — X-Tenant-Id: empresa1 + JWT de empresa1"
do_curl -X GET "$INVENTORY_URL/products" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "X-Tenant-Id: $TENANT_A"

assert_status "Inventory directo: header y JWT coinciden (empresa1)" "$HTTP_STATUS" "200"
echo "  Response: $HTTP_BODY"

# --- 2. CASO CONFIRMADO POR CÓDIGO: JWT de empresa1, pero header X-Tenant-Id: empresa2 ---
#     TenantFilter solo lee el header y no valida el claim del JWT, así que
#     se espera HTTP 200 aquí (Inventory deja pasar la petición). Esto NO es
#     un bug del script: es la confirmación reproducible de que el aislamiento
#     por JWT vive únicamente en el BFF (TenantAccessValidator), no en Inventory.
step "[CRÍTICO] JWT de empresa1 + header X-Tenant-Id: empresa2 (mismatch)"
do_curl -X GET "$INVENTORY_URL/products" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "X-Tenant-Id: $TENANT_B"

echo "  Status obtenido: $HTTP_STATUS"
if [ "$HTTP_STATUS" == "200" ]; then
    echo -e "  ${YELLOW}⚠ Inventory respondió 200, como se esperaba según TenantFilter.${NC}"
    echo -e "  ${YELLOW}  El servicio confía solo en X-Tenant-Id; el JWT no se valida a este nivel.${NC}"
    echo -e "  ${YELLOW}  La protección contra cruce de tenants depende 100% del BFF.${NC}"
    record_result "Inventory: valida JWT vs header en mismatch" "HALLAZGO" "HTTP $HTTP_STATUS - aislamiento depende del BFF"
else
    echo -e "  ${GREEN}✔ Inventory respondió $HTTP_STATUS: parece que se agregó validación de JWT en TenantFilter.${NC}"
    record_result "Inventory: valida JWT vs header en mismatch" "PASS" "HTTP $HTTP_STATUS"
fi
echo "  Response: $HTTP_BODY"

# --- 3. Acceso sin header X-Tenant-Id en absoluto ---
#     Confirmado en TenantFilter: si el header falta o está en blanco,
#     responde HTTP 400 con un JSON de error explícito.
step "Acceso sin header X-Tenant-Id"
do_curl -X GET "$INVENTORY_URL/products" \
  -H "Authorization: Bearer $TOKEN_A"

assert_status "Inventory: rechaza acceso sin X-Tenant-Id" "$HTTP_STATUS" "400"
echo "  Response: $HTTP_BODY"

# --- 4. Acceso sin token, solo con header ---
step "Acceso sin JWT, solo con X-Tenant-Id"
do_curl -X GET "$INVENTORY_URL/products" \
  -H "X-Tenant-Id: $TENANT_A"

assert_status "Inventory: rechaza acceso sin JWT" "$HTTP_STATUS" "401"
echo "  Response: $HTTP_BODY"

# --- 5. Health check (debe ser público según el README) ---
step "Health check — /actuator/health (público)"
do_curl -X GET "$INVENTORY_URL/actuator/health"

assert_status "Inventory: actuator/health es público" "$HTTP_STATUS" "200"
echo "  Response: $HTTP_BODY"
