# SmartLogix Backend For Frontend

Spring Boot BFF used as the public backend entry point for the Svelte frontend.

## Responsibilities

- Expose frontend-friendly endpoints under `/api`.
- Resolve the tenant from `/{tenant}/api/...`.
- Delegate register and login requests to `auth-service`.
- Validate JWT tokens on protected endpoints.
- Reject product requests when the JWT `tenant` claim does not match the path tenant.
- Forward product requests to `inventory-service`.
- Hide internal microservice URLs from the frontend.

## Run Locally

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The BFF runs on:

```text
http://localhost:8080
```

## Configuration

Default local values:

```text
SMARTLOGIX_AUTH_USERNAME=admin
SMARTLOGIX_AUTH_PASSWORD=admin123
SMARTLOGIX_JWT_SECRET=smartlogix-development-secret-key-change-me-2026
SMARTLOGIX_JWT_EXPIRATION_SECONDS=3600
AUTH_SERVICE_URL=http://localhost:8082
INVENTORY_SERVICE_URL=http://localhost:8081
```

The JWT secret must match the value configured in `auth-service` and `inventory-service`.

## Auth API

```http
POST /empresa1/api/auth/register
Content-Type: application/json

{
  "username": "demo",
  "password": "demo123"
}
```

```http
POST /empresa1/api/auth/login
Content-Type: application/json

{
  "username": "demo",
  "password": "demo123"
}
```

Response:

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "tenant": "empresa1"
}
```

The BFF sends the tenant to `auth-service` using `X-Tenant-Id`.

## Product API

The frontend should call these endpoints using the token returned by login:

```http
Authorization: Bearer <jwt>
```

Available endpoints:

```http
GET    /empresa1/api/products
GET    /empresa1/api/products/{id}
POST   /empresa1/api/products
PUT    /empresa1/api/products/{id}
PATCH  /empresa1/api/products/{id}/stock
DELETE /empresa1/api/products/{id}
```

The BFF checks that the token claim `tenant` matches `empresa1` from the path, then forwards these calls to `inventory-service` with `Authorization` and `X-Tenant-Id`.

## Docker

Recommended option from the backend root:

```bash
docker compose up --build
```

This starts the BFF, `inventory-service`, and PostgreSQL together.

Manual container build:

Build the image:

```bash
docker build -t smartlogix-bff .
```

Run the container:

```bash
docker run --rm -p 8080:8080 \
  -e SMARTLOGIX_JWT_SECRET=smartlogix-development-secret-key-change-me-2026 \
  -e AUTH_SERVICE_URL=http://host.docker.internal:8082 \
  -e INVENTORY_SERVICE_URL=http://host.docker.internal:8081 \
  smartlogix-bff
```
