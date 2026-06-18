# Contexto Para Codex - SmartLogix

## Estado Del Workspace

Workspace raiz:

```text
D:\01_Proyects\Programacion\Instituto\FullStackIII
```

Proyectos:

```text
Smart-Logix-Back/
Smart-Logix-Front/
```

El backend es el foco actual.

Repo Git real:

```text
Smart-Logix-Back/
```

La raiz `FullStackIII` no es repo Git.

## Flujo Git

El usuario trabaja con gitflow:

- `develop` como base.
- Una `feature/...` por hito.
- Commit en feature.
- Merge a `develop`.

Ya se hizo:

- `feature/auth-service` -> mergeada a `develop`.
- `feature/bff-auth-proxy` -> mergeada a `develop`.

El usuario indico que ya creo y checkout la rama para inventario:

```text
feature/inventory-tenancy
```

Ultimo estado conocido:

```text
feature/inventory-tenancy apunta al mismo commit que develop
develop esta ahead de origin/develop
```

Antes de editar, confirmar con:

```powershell
git status --short --branch
```

## Contexto Historico

Habia un `plan.md` creado en una sesion anterior. Esa sesion quedo incompleta:

- Se habia creado `auth-service`.
- Quedaba pendiente refactorizar BFF.
- Quedaba pendiente actualizar `inventory-service`.

En esta sesion ya se completo la refactorizacion del BFF y se mergeo a `develop`.

## Arquitectura Actual

Servicios backend:

- `BackendForFrontend`: entrada publica para frontend.
- `auth-service`: usuarios, tenants, registro, login, emision de JWT.
- `inventory-service`: productos y stock.

Puertos Docker/local:

- BFF: `8080`
- inventory-service: `8081`
- auth-service: `8082`
- PostgreSQL inventario: host `5432`, container `5432`
- PostgreSQL auth: host `5433`, container `5432`

## Decisiones Importantes Ya Tomadas

### Tenant en path publico

El frontend debe llamar al BFF con tenant en path:

```http
/{tenant}/api/...
```

Ejemplos:

```http
POST /empresa1/api/auth/login
POST /empresa1/api/auth/register
GET  /empresa1/api/products
```

### X-Tenant-Id

El BFF traduce el tenant del path a un header interno:

```http
X-Tenant-Id: empresa1
```

Este header se usa para comunicacion interna:

- BFF -> auth-service
- BFF -> inventory-service

Esto debe documentarse en README y mantenerse en el codigo. El usuario remarco que es importante no omitir esta decision.

### JWT

`auth-service` emite JWT HS256 con secreto compartido.

Claims relevantes:

- `sub`
- `tenant`
- `role`
- `scope`
- `iat`
- `exp`

El BFF valida el token y verifica que:

```text
claim tenant == {tenant} del path
```

Si no coincide, responde `403`.

Luego reenvia a inventario:

```http
Authorization: Bearer <jwt>
X-Tenant-Id: <tenant>
```

## Cambios Ya Implementados

### auth-service

Implementado:

- `POST /auth/register`
- `POST /auth/login`
- Tenants iniciales `empresa1` y `empresa2`.
- Usuarios demo por tenant.
- PostgreSQL propio.
- H2 para tests.
- Springdoc/OpenAPI.
- JaCoCo.

### BFF

Implementado:

- Auth local eliminada.
- `AuthService` ahora usa `RestClient` hacia `auth-service`.
- `RegisterRequest` y `RegisterResponse` agregados al BFF.
- `LoginResponse` incluye `tenant`.
- `TenantAccessValidator` agregado.
- `ForbiddenException` agregada.
- Productos movidos a `/{tenant}/api/products`.
- `InventoryClient` reenvia `Authorization` y `X-Tenant-Id`.
- `SecurityConfig` permite:
  - `/*/api/auth/login`
  - `/*/api/auth/register`
- `docker-compose.yml` incluye `auth-service` y `auth-postgres`.
- README raiz y README del BFF actualizados.

Pruebas ejecutadas:

```powershell
.\mvnw.cmd test
```

En:

- `BackendForFrontend`
- `auth-service`

Tambien:

```powershell
docker compose config
git diff --check
```

Todo paso correctamente en ese momento.

## Siguiente Hito: inventory-service

Objetivo inmediato:

Actualizar `inventory-service` para multi-tenancy real.

Estado actual de inventario:

- Endpoints internos siguen en `/products`.
- Requiere JWT.
- No lee `X-Tenant-Id`.
- Usa una tabla unica `products`.
- `sku` esta con `unique = true`, por lo que hoy es global, no por tenant.
- No hay Springdoc ni JaCoCo en inventario.

Implementacion recomendada para el hito:

- Crear manejo de contexto por request para tenant.
- Leer `X-Tenant-Id` con filtro/interceptor.
- Rechazar requests sin `X-Tenant-Id` con `400` o `401/403` segun decision del proyecto; preferible `400` si falta header interno requerido.
- Validar tenants permitidos inicialmente: `empresa1`, `empresa2`.
- Aislar persistencia por schema PostgreSQL:
  - `empresa1.products`
  - `empresa2.products`
- Crear schemas al iniciar o con SQL versionado/simple initializer.
- Configurar Hibernate para usar schema por request.
- Asegurar limpieza del contexto tenant al final del request.
- Ajustar unicidad SKU para que sea unica dentro del schema/tenant, no global entre tenants.
- Mantener endpoints internos `/products`; el tenant viene por header interno, no por path.
- Actualizar tests:
  - sin JWT -> `401`
  - sin `X-Tenant-Id` -> error esperado
  - crear producto en `empresa1`
  - mismo SKU permitido en `empresa2`
  - listar `empresa1` no muestra productos de `empresa2`
  - SKU duplicado dentro del mismo tenant falla
- Actualizar README de inventario explicando `X-Tenant-Id`.

## Pendientes De Rubrica Despues De Inventario

- Agregar Springdoc/OpenAPI a BFF.
- Agregar Springdoc/OpenAPI a inventory-service.
- Agregar JaCoCo a BFF.
- Agregar JaCoCo a inventory-service.
- Completar documentacion final de arquitectura:
  - BFF + auth-service + inventory-service.
  - PostgreSQL separado para auth/inventory.
  - Multi-tenancy path-based en BFF e interno por `X-Tenant-Id`.
  - Aislamiento por schemas en inventario.
  - Guia de ejecucion Docker.
  - Guia de pruebas y reportes de cobertura.
- Documentar pedidos y envios como roadmap/futura evolucion, no como alcance implementado.

## Preferencias Del Usuario

- Documentar claramente que se esta haciendo y por que.
- No ocultar decisiones tecnicas como `X-Tenant-Id`.
- Mantener el gitflow por feature branch.
- Priorizar cumplimiento de rubrica.
- El usuario esta a cargo del backend.

