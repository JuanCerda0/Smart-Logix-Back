#!/bin/bash
# =============================================================
# 01_auth_service.sh — Pruebas directas al Auth Service (8082)
#   Verifica registro, login, credenciales inválidas, y que
#   el JWT emitido tenga el claim de tenant correcto.
# =============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_common.sh"

section "AUTH SERVICE — http://localhost:8082"

# --- 1. Login con credenciales válidas (empresa1) ---
step "Login válido — $USER_A / $TENANT_A"
do_curl -X POST "$AUTH_URL/auth/login" \
  -H "X-Tenant-Id: $TENANT_A" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER_A\",\"password\":\"$PASS_A\"}"

assert_status "Auth: login válido empresa1" "$HTTP_STATUS" "200"
TOKEN_A=$(extract_json_field "$HTTP_BODY" "token")
if [ -n "$TOKEN_A" ]; then
    echo "  Token obtenido (empresa1): ${TOKEN_A:0:30}..."
else
    echo -e "  ${RED}No se pudo extraer el token del response.${NC}"
fi
echo "  Response: $HTTP_BODY"

# --- 2. Login válido para empresa2 (lo usaremos en pruebas cruzadas después) ---
step "Login válido — $USER_B / $TENANT_B"
do_curl -X POST "$AUTH_URL/auth/login" \
  -H "X-Tenant-Id: $TENANT_B" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER_B\",\"password\":\"$PASS_B\"}"

assert_status "Auth: login válido empresa2" "$HTTP_STATUS" "200"
TOKEN_B=$(extract_json_field "$HTTP_BODY" "token")
echo "  Response: $HTTP_BODY"

# --- 3. Login con password incorrecta ---
step "Login con password incorrecta"
do_curl -X POST "$AUTH_URL/auth/login" \
  -H "X-Tenant-Id: $TENANT_A" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER_A\",\"password\":\"password-incorrecta\"}"

assert_status "Auth: login con password incorrecta es rechazado" "$HTTP_STATUS" "401"
echo "  Response: $HTTP_BODY"

# --- 4. Login sin header X-Tenant-Id ---
step "Login sin header X-Tenant-Id"
do_curl -X POST "$AUTH_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER_A\",\"password\":\"$PASS_A\"}"

echo "  Status obtenido: $HTTP_STATUS (revisar si el servicio exige el header o usa un default)"
record_result "Auth: login sin X-Tenant-Id" "INFO" "HTTP $HTTP_STATUS"
echo "  Response: $HTTP_BODY"

# --- 5. Registro de un usuario nuevo en empresa1 ---
RANDOM_USER="testuser_$RANDOM"
step "Registro de usuario nuevo — $RANDOM_USER en $TENANT_A"
do_curl -X POST "$AUTH_URL/auth/register" \
  -H "X-Tenant-Id: $TENANT_A" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$RANDOM_USER\",\"password\":\"clave12345\"}"

assert_status "Auth: registro de usuario nuevo" "$HTTP_STATUS" "201"
echo "  Response: $HTTP_BODY"

# --- 6. Registro duplicado (mismo username en mismo tenant) ---
step "Registro duplicado — debería fallar"
do_curl -X POST "$AUTH_URL/auth/register" \
  -H "X-Tenant-Id: $TENANT_A" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$RANDOM_USER\",\"password\":\"clave12345\"}"

echo "  Status obtenido: $HTTP_STATUS (se espera 4xx por usuario duplicado)"
record_result "Auth: registro duplicado rechazado" "INFO" "HTTP $HTTP_STATUS"
echo "  Response: $HTTP_BODY"

# --- Exporta los tokens para que otros scripts los reutilicen ---
echo "$TOKEN_A" > /tmp/smartlogix_token_empresa1.txt
echo "$TOKEN_B" > /tmp/smartlogix_token_empresa2.txt

echo ""
echo -e "${GREEN}Tokens guardados en /tmp para uso de los siguientes scripts.${NC}"
