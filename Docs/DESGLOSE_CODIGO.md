# Desglose de Código y Estructura - SmartLogix Backend

Este documento detalla la estructura de archivos, responsabilidades y patrones de diseño utilizados en los tres microservicios del proyecto.

## 1. Backend For Frontend (BFF)
Ubicación: `Smart-Logix-Back/BackendForFrontend`
Puerto: `8080`

El BFF es el orquestador y la única cara pública. Su estructura se organiza por dominios funcionales.

### Estructura de Paquetes
*   `smartlogix.BackendForFrontend`
    *   `auth/`: Gestión de seguridad y proxy de autenticación.
        *   `AuthController.java`: Expone endpoints `/{tenant}/api/auth/login` y `/register`.
        *   `AuthService.java`: Usa `RestClient` para delegar la autenticación al `auth-service`.
        *   `JwtTokenService.java`: Valida y decodifica los tokens JWT recibidos.
        *   `TenantAccessValidator.java`: **Lógica Crítica.** Valida que el `tenant` en el JWT coincida con el `{tenant}` de la URL.
    *   `product/`: Proxy hacia el inventario.
        *   `ProductController.java`: Expone `/{tenant}/api/products`.
        *   `InventoryClient.java`: Cliente REST que reenvía peticiones a `inventory-service` inyectando el header `X-Tenant-Id`.
    *   `config/`:
        *   `SecurityConfig.java`: Configuración de Spring Security (permite login/register público, protege el resto).
        *   `RestClientConfig.java`: Configuración de los clientes HTTP para comunicación interna.
    *   `shared/`: Excepciones globales y utilidades comunes.

---

## 2. Auth Service
Ubicación: `Smart-Logix-Back/auth-service`
Puerto: `8082`

Responsable de la persistencia de usuarios y generación de identidad.

### Estructura de Paquetes
*   `smartlogix.auth_service`
    *   `auth/`: El núcleo de identidad.
        *   `Tenant.java` & `UserAccount.java`: Entidades JPA que representan la base de datos de usuarios.
        *   `AuthService.java`: Lógica de negocio para registro y validación de contraseñas.
        *   `JwtTokenService.java`: Genera los JWT firmados con el secreto compartido.
        *   `AuthDataInitializer.java`: Seed de datos (crea `empresa1` y `empresa2` al arrancar).
    *   `config/`:
        *   `SecurityConfig.java`: Configura el servicio para ser consumido internamente.
    *   `shared/`: Manejo de errores específicos de autenticación.

---

## 3. Inventory Service
Ubicación: `Smart-Logix-Back/inventory-service`
Puerto: `8081`

Servicio de dominio encargado de los productos. Actualmente en proceso de refactorización multi-tenant.

### Estructura de Paquetes
*   `smartlogix.inventory_service`
    *   `product/`:
        *   `Product.java`: Entidad que representa un producto en la DB.
        *   `ProductController.java`: Endpoints internos `/products`. No usa tenant en path, lo espera por header.
        *   `ProductService.java`: Lógica de stock y validaciones de negocio.
        *   `ProductRepository.java`: Interfaz de Spring Data JPA para PostgreSQL.
    *   `config/`:
        *   `SecurityConfig.java`: Valida el JWT para asegurar que solo servicios autorizados (como el BFF) accedan.
    *   `shared/`: Utilidades de dominio.

---

## Patrones de Diseño Identificados

1.  **BFF (Backend For Frontend):** Utilizado para desacoplar el frontend de la complejidad de los microservicios internos y manejar la seguridad en un solo lugar.
2.  **Proxy / Gateway:** El BFF actúa como un proxy inteligente que enriquece las peticiones con datos de tenant.
3.  **DTO (Data Transfer Object):** Cada servicio tiene sus propios `Request` y `Response` para no exponer las entidades de base de datos directamente.
4.  **Repository Pattern:** Uso de Spring Data JPA para abstraer el acceso a datos.
5.  **Shared Secret:** Los tres servicios comparten una clave secreta para validar la autenticidad de los JWT sin necesidad de consultar constantemente al servicio de Auth.

## Comunicación Inter-Servicio

```text
[Frontend] 
    |
    | (HTTPS / JWT / {tenant} in Path)
    v
[BFF] ---------------------> [Auth Service]
    |      (X-Tenant-Id)
    |
    | (Authorization + X-Tenant-Id)
    v
[Inventory Service]
```

## Próximos Objetivos de Estudio
1.  **`TenantAccessValidator` en el BFF:** Entender cómo se bloquea el acceso si un usuario de `empresa1` intenta entrar a `empresa2`.
2.  **`InventoryClient` en el BFF:** Ver cómo se construye la URL dinámica hacia el inventario.
3.  **Configuración de JPA en Inventory:** Preparar el terreno para el aislamiento por schemas.
