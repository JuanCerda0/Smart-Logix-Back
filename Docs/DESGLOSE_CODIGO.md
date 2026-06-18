# Desglose de Código y Estructura Final - SmartLogix

Este documento refleja la estructura final del código tras la implementación del multi-tenancy y las herramientas de calidad.

## 1. Backend For Frontend (BFF)
*   **`auth/TenantAccessValidator.java`**: Valida que el tenant del token JWT coincida con el de la URL. Previene que un usuario de la Empresa A vea datos de la Empresa B.
*   **`product/InventoryClient.java`**: Cliente REST que propaga el token de seguridad y el header `X-Tenant-Id` hacia el microservicio de inventario.
*   **`config/SecurityConfig.java`**: Configura el acceso público a Swagger (`/swagger-ui/**`) y protege los endpoints de negocio.

## 2. Auth Service
*   **`auth/AuthDataInitializer.java`**: Crea automáticamente los esquemas y datos de prueba para `empresa1` y `empresa2` al iniciar.
*   **`auth/JwtTokenService.java`**: Genera el token con el claim `tenant` necesario para el aislamiento.

## 3. Inventory Service (Refactorizado)
*   **`tenant/TenantFilter.java`**: Extrae el `X-Tenant-Id` de cada petición y lo pone en un `TenantContext` seguro (ThreadLocal).
*   **`tenant/SchemaMultiTenantConnectionProvider.java`**: **Lógica de Aislamiento.** Ejecuta `SET search_path TO <tenant>` en cada conexión a la base de datos.
*   **`tenant/CurrentTenantResolver.java`**: Informa a Hibernate sobre cuál es el tenant activo para la consulta actual.
*   **`tenant/TenantSchemaInitializer.java`**: Garantiza que las tablas de productos existan en todos los esquemas de las empresas registradas.

---

## Flujo de una Petición Multi-Tenant

1.  **Entrada**: `GET /empresa1/api/products` -> BFF.
2.  **BFF**: Valida JWT -> Extrae "empresa1" -> Llama a Inventario con header `X-Tenant-Id: empresa1`.
3.  **Inventario**: `TenantFilter` captura el header -> Hibernate cambia el schema a `empresa1` -> Se ejecuta `SELECT * FROM empresa1.products`.
4.  **Resultado**: Los datos están físicamente aislados a nivel de base de datos.

## Herramientas de Calidad Implementadas
*   **Swagger (OpenAPI 3.0.3)**: Disponible en el puerto de cada servicio bajo `/swagger-ui.html`.
*   **JaCoCo**: Genera reportes de cobertura tras ejecutar `mvnw test`. Los resultados se encuentran en `target/site/jacoco/index.html`.
*   **Tests de Aislamiento**: Localizados en `ProductControllerTests.java` (Inventory), verifican que no haya cruce de información entre esquemas.
