# Contexto Humano - SmartLogix Backend

## Situacion Inicial

Este workspace contiene dos proyectos:

- `Smart-Logix-Back`: backend Java/Spring Boot.
- `Smart-Logix-Front`: frontend Svelte.

El trabajo actual corresponde principalmente al backend. En una sesion anterior habia quedado un plan general en `plan.md`; esa ejecucion no se termino. El punto donde habia quedado era:

- `auth-service` ya estaba creado como microservicio nuevo.
- Quedaba pendiente refactorizar el BFF para dejar de autenticar localmente.
- Tambien quedaba pendiente actualizar `inventory-service` para soportar multi-tenancy real.

El flujo de ramas acordado es gitflow simple:

- Partir desde `develop`.
- Crear una rama `feature/...` por hito.
- Commit del hito en su rama.
- Merge de la feature a `develop`.

## Que Se Hizo Hasta Ahora

### 1. Auth service

Se integro `auth-service` como microservicio independiente.

Responsabilidades actuales:

- Manejar tenants.
- Registrar usuarios.
- Autenticar usuarios.
- Emitir JWT firmados con secreto compartido.

Endpoints internos del servicio:

```http
POST /auth/register
POST /auth/login
```

Este servicio no recibe el tenant en la URL. El tenant se envia por header:

```http
X-Tenant-Id: empresa1
```

Esto es importante: `X-Tenant-Id` es la forma en que el BFF comunica al `auth-service` que tenant resolvio desde el path publico.

### 2. BFF refactorizado

Antes el BFF tenia login local con credenciales configuradas por variables tipo `SMARTLOGIX_AUTH_USERNAME` y `SMARTLOGIX_AUTH_PASSWORD`.

Eso fue reemplazado. Ahora el BFF:

- Es la unica entrada publica para el frontend.
- Expone rutas tenant-aware.
- Delega login y registro al `auth-service`.
- Valida JWT para endpoints protegidos.
- Valida que el claim `tenant` del JWT coincida con el tenant del path.
- Reenvia requests de productos al `inventory-service`.

Rutas publicas principales:

```http
POST /{tenant}/api/auth/register
POST /{tenant}/api/auth/login
GET  /{tenant}/api/products
```

Ejemplo:

```http
POST /empresa1/api/auth/login
```

Cuando el BFF llama al `auth-service`, envia:

```http
X-Tenant-Id: empresa1
```

Cuando el BFF llama al `inventory-service`, envia:

```http
Authorization: Bearer <jwt>
X-Tenant-Id: empresa1
```

El header `X-Tenant-Id` es clave para la siguiente etapa: inventario debe usarlo para decidir en que tenant/schema trabajar.

### 3. Docker Compose

`docker-compose.yml` fue actualizado para levantar:

- `bff` en `localhost:8080`
- `inventory-service` en `localhost:8081`
- `auth-service` en `localhost:8082`
- PostgreSQL de inventario en `localhost:5432`
- PostgreSQL de autenticacion en `localhost:5433`

El BFF usa:

```text
AUTH_SERVICE_URL=http://auth-service:8082
INVENTORY_SERVICE_URL=http://inventory-service:8081
```

### 4. Pruebas realizadas

Se ejecutaron pruebas Maven:

```bash
BackendForFrontend/mvnw.cmd test
auth-service/mvnw.cmd test
```

Ambas suites pasaron.

Tambien se valido:

```bash
docker compose config
```

## Tecnologias y Herramientas Usadas

- Java 21.
- Spring Boot 4.
- Spring Web MVC.
- Spring Security.
- Spring OAuth2 Resource Server para validar JWT.
- JWT HS256 con secreto compartido entre `auth-service`, BFF e `inventory-service`.
- Spring Data JPA para persistencia.
- Bean Validation para validar DTOs.
- PostgreSQL para ejecucion local/Docker.
- H2 para tests automatizados donde aplica.
- Docker y Docker Compose para levantar servicios.
- Maven Wrapper por microservicio.
- Spring `RestClient` para comunicacion HTTP entre BFF y microservicios.
- Springdoc/OpenAPI ya incorporado en `auth-service`.
- JaCoCo ya incorporado en `auth-service`.

## Que Falta

El siguiente hito es actualizar `inventory-service`.

Pendiente principal:

- Leer `X-Tenant-Id` en cada request.
- Rechazar requests sin tenant.
- Aislar productos por tenant.
- Implementar aislamiento por schema PostgreSQL, por ejemplo:
  - `empresa1.products`
  - `empresa2.products`
- Permitir que el mismo SKU exista en tenants distintos.
- Mantener SKU unico dentro del mismo tenant/schema.
- Agregar tests de aislamiento.
- Actualizar README de inventario con el uso de `X-Tenant-Id`.

Despues de inventario, todavia queda:

- Agregar Swagger/OpenAPI a BFF e inventario.
- Agregar JaCoCo a BFF e inventario.
- Completar documentacion final de arquitectura, ejecucion, pruebas y multi-tenancy.
- Dejar pedidos y envios documentados como evolucion futura, no como implementacion actual.

