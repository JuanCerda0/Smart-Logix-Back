# Estado Actual del Backend - SmartLogix

## 1. Arquitectura del Sistema

El backend está compuesto por tres microservicios desarrollados en **Java 21** con **Spring Boot 3.x**, comunicándose de forma síncrona mediante REST.

### Componentes Principales

1.  **Backend For Frontend (BFF) - Puerto 8080**:
    *   Es el único punto de entrada público para el frontend.
    *   Gestiona el multi-tenancy basado en el path: `/{tenant}/api/...`.
    *   Valida los tokens JWT y asegura que el claim `tenant` coincida con el tenant en la URL.
    *   Propaga el header interno `X-Tenant-Id` a los servicios internos.
    *   Actúa como proxy para `auth-service` (auth) e `inventory-service` (productos).

2.  **Auth Service - Puerto 8082**:
    *   Gestiona usuarios, tenants y sesiones.
    *   Responsable del registro y login de usuarios por tenant.
    *   Emite tokens JWT firmados con una clave secreta compartida (HS256).
    *   Tiene su propia base de datos PostgreSQL (`smartlogix_auth`).
    *   Ya incluye **Swagger/OpenAPI** y reportes de cobertura con **JaCoCo**.

3.  **Inventory Service - Puerto 8081**:
    *   Gestiona el catálogo de productos y el stock.
    *   Actualmente ofrece un CRUD básico de productos.
    *   Tiene su propia base de datos PostgreSQL (`smartlogix_inventory`).
    *   **Estado Crítico**: Aún no es multi-tenant. Los productos son globales y no están aislados por empresa.
    *   Pendiente: Incorporar Swagger y JaCoCo.

---

## 2. Flujo de Datos y Multi-Tenancy

### Registro y Login
1.  Frontend llama a `POST /empresa1/api/auth/login`.
2.  BFF extrae `empresa1`, lo añade al header `X-Tenant-Id` y llama al `auth-service`.
3.  `auth-service` valida las credenciales contra el tenant recibido.
4.  Se devuelve un JWT que contiene `tenant: empresa1`.

### Acceso a Productos
1.  Frontend llama a `GET /empresa1/api/products` con el JWT.
2.  BFF verifica que el JWT sea válido y que su claim `tenant` sea `empresa1`.
3.  BFF reenvía la petición al `inventory-service` incluyendo:
    *   `Authorization: Bearer <token>`
    *   `X-Tenant-Id: empresa1`
4.  **Próximo paso**: El `inventory-service` debe usar `X-Tenant-Id` para cambiar el schema de la base de datos a `empresa1`.

---

## 3. Estado de Cumplimiento (Rúbrica vs. Realidad)

| Requerimiento | Estado | Notas |
| :--- | :---: | :--- |
| Arquitectura Microservicios | ✅ | BFF + 2 Microservicios (Auth e Inventory). |
| Multi-tenancy (Path-based) | ✅ | Implementado en el BFF. |
| Aislamiento de Datos | ⚠️ | Hecho en Auth, **Pendiente** en Inventory (Schema-based). |
| Comunicación REST | ✅ | Uso de `RestClient` y comunicación inter-servicio. |
| Persistencia Real | ✅ | PostgreSQL configurado para cada servicio. |
| Documentación Swagger | ⚠️ | Solo en `auth-service`. Falta en BFF e Inventory. |
| Cobertura JaCoCo (>60%) | ⚠️ | Solo en `auth-service`. Falta en BFF e Inventory. |
| Docker Compose | ✅ | Configurado y funcional para todos los servicios. |
| Pedidos y Envíos | ℹ️ | Definidos como "Roadmap" en la documentación. |

---

## 4. Próximos Pasos Recomendados

1.  **Refactorizar Inventory Service para Multi-Tenancy**:
    *   Implementar un interceptor para capturar `X-Tenant-Id`.
    *   Configurar Hibernate para usar `MultiTenancyStrategy.SCHEMA`.
    *   Asegurar que el SKU sea único solo dentro de cada schema.

2.  **Completar Herramientas de Desarrollo**:
    *   Agregar dependencias de `springdoc-openapi` al BFF e Inventory.
    *   Configurar el plugin de JaCoCo en los `pom.xml` restantes.

3.  **Documentación Final**:
    *   Generar diagramas de arquitectura.
    *   Actualizar los README con ejemplos de uso del header `X-Tenant-Id`.
