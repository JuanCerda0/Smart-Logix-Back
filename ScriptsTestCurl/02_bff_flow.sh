#!/bin/bash
# =============================================================
# 02_bff_flow.sh — Pruebas end-to-end a través del BFF (8080)
#   Cubre: login vía BFF, CRUD de productos, y el caso clave
#   de la rúbrica: que el BFF rechace cruce de tenants.
# =============================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/_common.sh"

section "BFF — http://localhost:8080 (flujo completo frontend -> BFF -> servicios)"

# --- 1. Login vía BFF para empresa1 ---
step "Login vía BFF — /$TENANT_A/api/auth/login"
do_curl -X POST "$BFF_URL/$TENANT_A/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER_A\",\"password\":\"$PASS_A\"}"

assert_status "BFF: login empresa1 vía path" "$HTTP_STATUS" "200"
TOKEN_A=$(extract_json_field "$HTTP_BODY" "token")
echo "  Response: $HTTP_BODY"

# --- 2. Login vía BFF para empresa2 ---
step "Login vía BFF — /$TENANT_B/api/auth/login"
do_curl -X POST "$BFF_URL/$TENANT_B/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USER_B\",\"password\":\"$PASS_B\"}"

assert_status "BFF: login empresa2 vía path" "$HTTP_STATUS" "200"
TOKEN_B=$(extract_json_field "$HTTP_BODY" "token")
echo "  Response: $HTTP_BODY"

if [ -z "$TOKEN_A" ] || [ -z "$TOKEN_B" ]; then
    echo -e "${RED}No se pudieron obtener ambos tokens. Abortando el resto del script.${NC}"
    exit 1
fi

# --- 3. Crear un producto en empresa1 ---
step "Crear producto en empresa1"
do_curl -X POST "$BFF_URL/$TENANT_A/api/products" \
  -H "Authorization: Bearer $TOKEN_A" \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU-TEST-001","name":"Mouse Inalambrico Test","description":"Producto creado por script de prueba","category":"Accesorios","unitPrice":12990,"stock":25}'

assert_status "BFF: crear producto en empresa1" "$HTTP_STATUS" "201"
PRODUCT_ID=$(extract_json_field "$HTTP_BODY" "id")
echo "  Producto creado con id: $PRODUCT_ID"
echo "  Response: $HTTP_BODY"

# --- 4. Listar productos de empresa1 (debería incluir el recién creado) ---
step "Listar productos de empresa1"
do_curl -X GET "$BFF_URL/$TENANT_A/api/products" \
  -H "Authorization: Bearer $TOKEN_A"

assert_status "BFF: listar productos empresa1" "$HTTP_STATUS" "200"
echo "  Response: $HTTP_BODY"

# --- 5. CASO CLAVE: empresa2 intenta listar productos usando la URL de empresa1 ---
#     pero con SU PROPIO token (token de empresa2). Esto es lo que debe bloquear
#     el TenantAccessValidator del BFF según la documentación.
step "[AISLAMIENTO] Token de empresa2 intentando acceder a /$TENANT_A/api/products"
do_curl -X GET "$BFF_URL/$TENANT_A/api/products" \
  -H "Authorization: Bearer $TOKEN_B"

assert_status "BFF: bloquea cruce de tenant (token B sobre path A)" "$HTTP_STATUS" "403"
echo "  Response: $HTTP_BODY"

# --- 6. CASO CLAVE: acceso sin token alguno ---
step "[SEGURIDAD] Acceso a productos sin token"
do_curl -X GET "$BFF_URL/$TENANT_A/api/products"

assert_status "BFF: rechaza acceso sin token" "$HTTP_STATUS" "401"
echo "  Response: $HTTP_BODY"

# --- 7. CASO CLAVE: token con firma inválida / manipulado ---
step "[SEGURIDAD] Acceso con token manipulado"
do_curl -X GET "$BFF_URL/$TENANT_A/api/products" \
  -H "Authorization: Bearer ${TOKEN_A}manipulado"

assert_status "BFF: rechaza token manipulado" "$HTTP_STATUS" "401"
echo "  Response: $HTTP_BODY"

# --- 8. Actualizar stock del producto creado ---
if [ -n "$PRODUCT_ID" ]; then
    step "Actualizar stock del producto $PRODUCT_ID"
    do_curl -X PATCH "$BFF_URL/$TENANT_A/api/products/$PRODUCT_ID/stock" \
      -H "Authorization: Bearer $TOKEN_A" \
      -H "Content-Type: application/json" \
      -d '{"stock": 40}'

    assert_status "BFF: actualizar stock" "$HTTP_STATUS" "200"
    echo "  Response: $HTTP_BODY"

    # --- 9. Eliminar el producto de prueba (limpieza) ---
    step "Eliminar producto de prueba $PRODUCT_ID"
    do_curl -X DELETE "$BFF_URL/$TENANT_A/api/products/$PRODUCT_ID" \
      -H "Authorization: Bearer $TOKEN_A"

    assert_status "BFF: eliminar producto" "$HTTP_STATUS" "204"
    echo "  Response: $HTTP_BODY"
else
    echo -e "${YELLOW}Se omiten los pasos 8 y 9 porque no se obtuvo PRODUCT_ID en el paso 3.${NC}"
fi

# --- Exporta tokens para el script de aislamiento directo a Inventory ---
echo "$TOKEN_A" > /tmp/smartlogix_token_empresa1.txt
echo "$TOKEN_B" > /tmp/smartlogix_token_empresa2.txt
