# SmartLogix - Grupo 9
**Plataforma de Gestión Logística para eCommerce**

---

## Descripción

SmartLogix es un proyecto enfocado en el desarrollo de una plataforma para mejorar la gestión logística de pequeñas y medianas empresas de comercio electrónico.

Surge como respuesta a las limitaciones actuales de muchos negocios, que aún dependen de procesos manuales o sistemas poco flexibles para manejar inventarios, pedidos y envíos.

---

## Problema

Las PYMEs de eCommerce suelen enfrentar:

- Desorden en la gestión de inventario  
- Errores en el procesamiento de pedidos  
- Retrasos en envíos  
- Dificultad para escalar operaciones  

Esto impacta directamente en la eficiencia y en la experiencia del cliente.

---

## Objetivo

Desarrollar una solución que permita:

- Centralizar la operación logística  
- Reducir errores manuales  
- Mejorar la organización y seguimiento de procesos  
- Facilitar el crecimiento del negocio  

---

## Alcance Inicial

El proyecto considera abordar:

- Inventario  
- Pedidos  
- Envíos  
- Gestión básica de usuarios  

---

## Estado del Proyecto

Backend implementado con:

- Backend For Frontend en `BackendForFrontend`
- Microservicio de autenticacion en `auth-service`
- Microservicio de inventario en `inventory-service`
- PostgreSQL separado para autenticacion e inventario mediante Docker Compose
- H2 solo para tests automatizados del microservicio de inventario
- Rutas tenant-aware bajo `/{tenant}/api/...`

---

## Ejecutar Backend Completo

Desde esta carpeta:

```bash
docker compose up --build
```

Esto levanta:

- PostgreSQL de inventario en `localhost:5432`
- PostgreSQL de autenticacion en `localhost:5433`
- `auth-service` en `http://localhost:8082`
- `inventory-service` en `http://localhost:8081`
- `BackendForFrontend` en `http://localhost:8080`

El frontend debe consumir el BFF:

```text
http://localhost:8080
```

No debe consumir directamente `inventory-service`.

## Flujo Actual

El frontend consume solo el BFF usando el tenant en el path:

```text
http://localhost:8080/empresa1/api
```

El BFF resuelve `{tenant}`, lo envia a `auth-service` como `X-Tenant-Id` para login y registro, y valida que los JWT usados en productos tengan un claim `tenant` que coincida con el tenant del path. Para productos, el BFF reenvia `Authorization` y `X-Tenant-Id` a `inventory-service`.

Endpoints principales:

```http
POST /{tenant}/api/auth/register
POST /{tenant}/api/auth/login
GET  /{tenant}/api/products
```
