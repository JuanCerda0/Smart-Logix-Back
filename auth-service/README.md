# SmartLogix Auth Service

Microservicio responsable de tenants, usuarios, registro, login y emision de JWT.

## Run Locally

```bash
mvnw.cmd spring-boot:run
```

El servicio corre en:

```text
http://localhost:8082
```

## Persistencia

Usa PostgreSQL propio. Valores locales por defecto:

```text
SMARTLOGIX_AUTH_DB_URL=jdbc:postgresql://localhost:5433/smartlogix_auth
SMARTLOGIX_AUTH_DB_USERNAME=smartlogix
SMARTLOGIX_AUTH_DB_PASSWORD=smartlogix123
```

## Tenants Demo

El servicio crea datos iniciales para:

```text
empresa1 / admin / admin123
empresa2 / admin / admin123
```

## API

El BFF envia el tenant resuelto desde el path mediante `X-Tenant-Id`.

```http
POST /auth/register
X-Tenant-Id: empresa1
Content-Type: application/json

{
  "username": "admin2",
  "password": "admin123"
}
```

```http
POST /auth/login
X-Tenant-Id: empresa1
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

Swagger:

```text
http://localhost:8082/swagger-ui.html
```

## Tests y Cobertura

```bash
mvnw.cmd test
```

Reporte JaCoCo:

```text
target/site/jacoco/index.html
```
