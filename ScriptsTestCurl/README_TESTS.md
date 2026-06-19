# SmartLogix — Scripts de Prueba de Integración (curl)

Scripts de bash que verifican, con peticiones HTTP reales, que los 3 servicios
del backend (BFF, Auth, Inventory) se comunican correctamente y que el
aislamiento multi-tenant funciona como se documentó.

## Requisitos

- Tener los servicios corriendo con:
  ```bash
  docker compose up --build
  ```
- `curl` instalado (viene por defecto en la mayoría de distros Linux/macOS).
  En Windows, usar Git Bash o WSL.

## Archivos

| Archivo | Qué prueba |
|---|---|
| `_common.sh` | Variables y funciones compartidas. No se ejecuta solo. |
| `01_auth_service.sh` | Login, registro, credenciales inválidas — directo al Auth Service (8082). |
| `02_bff_flow.sh` | Flujo completo Frontend→BFF→servicios: login, CRUD de productos, y el caso clave: que el BFF **bloquee** el cruce de tenants (token de empresa2 contra URL de empresa1). |
| `03_inventory_direct.sh` | Pega **directo** a Inventory (8081), sin pasar por el BFF. Sirve para confirmar empíricamente si Inventory valida el JWT contra el header `X-Tenant-Id`, o si confía solo en el header. |
| `run_all.sh` | Corre todo en orden y muestra un resumen final con conteo PASS/FAIL. |

## Uso

```bash
chmod +x *.sh
./run_all.sh
```

También puedes correr cada script por separado si quieres revisar un flujo
específico:

```bash
./01_auth_service.sh
./02_bff_flow.sh
./03_inventory_direct.sh
```

### ⚠️ Resultado esperado y ya confirmado en el test `[CRÍTICO]`

Revisando el código real de `TenantFilter` (Inventory Service), el filtro
**solo lee el header `X-Tenant-Id`** y nunca decodifica ni compara el claim
`tenant` del JWT:

```java
String tenant = request.getHeader(TENANT_HEADER);
if (tenant == null || tenant.isBlank()) { ... }
if (!tenantProperties.isAllowed(tenant)) { ... }
TenantContext.setTenant(tenant.toLowerCase());
```

Por lo tanto, el test #2 de `03_inventory_direct.sh` (JWT de empresa1 +
header `X-Tenant-Id: empresa2`) **va a devolver HTTP 200** y el script lo va
a marcar como `FAIL (inseguro si se accede directo)` en el resumen final.

**Esto es el resultado esperado, no un error del script ni del backend.**
Es la prueba confirmando, de forma reproducible, que:

- El aislamiento de tenant por JWT se aplica únicamente en el BFF
  (`TenantAccessValidator`).
- Inventory Service, si se le llama directamente sin pasar por el BFF,
  confía ciegamente en el header `X-Tenant-Id` y no detecta el mismatch.

## Notas

- Los scripts asumen los tenants demo `empresa1` y `empresa2` con usuario
  `admin` / `admin123`, tal como están documentados en los README de Auth e
  Inventory.
- Los tokens JWT generados se guardan temporalmente en `/tmp/` para que
  `03_inventory_direct.sh` los reutilice sin tener que loguearse de nuevo.
