# Plan de Finalización Backend - SmartLogix

Este plan detalla los pasos necesarios para completar la refactorización del microservicio de inventario hacia un modelo multi-tenant real y cumplir con todos los criterios de la rúbrica de evaluación.

## Fase 1: Multi-Tenancy en Inventory Service (Aislamiento por Schemas)
El objetivo es que cada empresa (`empresa1`, `empresa2`) tenga su propio espacio aislado en la base de datos PostgreSQL.

### 1.1 Gestión de Contexto (Thread-Safe)
*   Implementar `TenantContext` usando `ThreadLocal` para mantener el ID del tenant durante el procesamiento de la petición.
*   Crear un `TenantFilter` que extraiga el header `X-Tenant-Id` y lo valide antes de guardarlo en el contexto.

### 1.2 Configuración de Hibernate
*   Implementar `SchemaTenantIdentifierResolver`: para decirle a Hibernate qué schema usar en cada momento.
*   Implementar `MultiTenantConnectionProviderImpl`: para ejecutar el comando `SET search_path TO ...` en cada conexión obtenida.
*   Configurar estas piezas en la clase de configuración de Hibernate/JPA.

### 1.3 Infraestructura de Base de Datos
*   Actualizar `docker-compose.yml` o crear un script de inicialización (`schema.sql`) para asegurar que los schemas `empresa1` y `empresa2` se creen al iniciar el contenedor.
*   Verificar que las tablas se creen correctamente en cada schema al arrancar la aplicación.

---

## Fase 2: Calidad y Cumplimiento de Rúbrica
El objetivo es estandarizar el nivel de calidad en los tres servicios del backend.

### 2.1 Swagger / OpenAPI
*   Añadir la dependencia `springdoc-openapi` al **BFF** y al **Inventory Service**.
*   Configurar los beans necesarios para que la documentación sea accesible y descriptiva (títulos, versiones, descripciones de endpoints).

### 2.2 Cobertura de Código (JaCoCo)
*   Configurar el plugin de JaCoCo en los archivos `pom.xml` del **BFF** y del **Inventory Service**.
*   Establecer la meta de cobertura mínima del 60%, similar a como ya está configurado en `auth-service`.

---

## Fase 3: Pruebas y Validación Final
Garantizar que el sistema es robusto y cumple lo prometido.

### 3.1 Tests de Aislamiento
*   Desarrollar tests de integración que verifiquen que un producto creado bajo `empresa1` no es visible para `empresa2`.
*   Verificar que el mismo SKU pueda coexistir en distintos schemas sin conflictos de unicidad.

### 3.2 Ejecución de Reportes
*   Correr la suite completa de tests en los tres servicios.
*   Generar y verificar los reportes HTML de JaCoCo (`target/site/jacoco/index.html`).

---

## Fase 4: Documentación de Arquitectura
Preparar el material final para la entrega.

*   Crear un diagrama de arquitectura que muestre el flujo desde el Frontend hasta los microservicios y sus bases de datos.
*   Documentar el uso del header `X-Tenant-Id` como pieza central de la comunicación interna.
*   Actualizar los README de cada servicio con instrucciones claras de ejecución y pruebas.

---

## Orden de Ejecución Recomendado
1.  **Inventory Service Multi-Tenancy** (Es la tarea más compleja y prioritaria).
2.  **Configuración de Swagger** en todos los servicios (Fácil cumplimiento).
3.  **Configuración de JaCoCo** y escritura de tests faltantes.
4.  **Documentación Final**.
