# Guía de Integración — Frontend (Svelte) ↔ BFF SmartLogix

Esta guía es para conectar el frontend al backend. Está basada en pruebas
reales ya ejecutadas contra el BFF (ver scripts de prueba), así que los
ejemplos de aquí están confirmados funcionando.

## 1. URL base y arranque

El BFF corre en:

```
http://localhost:8080
```

Antes de programar nada, levanta el backend completo con:

```bash
docker compose up --build
```

Y confirma que responde (deberías ver la UI de Swagger):

```
http://localhost:8080/swagger-ui.html
```

## 2. ⚠️ Lo primero que hay que resolver: CORS

El frontend (Vite/Svelte) corre típicamente en `http://localhost:5173` (o el
puerto que asigne Vite), que es un **origen distinto** al del BFF
(`localhost:8080`). Los navegadores bloquean estas peticiones por defecto a
menos que el backend declare explícitamente que las acepta.

**Antes de programar el primer fetch**, confirma con quien lleva el backend
si `SecurityConfig` del BFF ya tiene un bean `CorsConfigurationSource` o
`@CrossOrigin` habilitado para el origen de Vite. Si no está, el primer
intento de login desde el navegador va a fallar en consola con un error de
tipo:

```
Access to fetch at 'http://localhost:8080/...' from origin 'http://localhost:5173'
has been blocked by CORS policy
```

Esto **no se ve con curl ni con Postman** porque esas herramientas no
aplican política de origen — por eso las pruebas de backend pasaron 100%
pero esto puede sorprender al primer intento real desde el navegador.

## 3. Estructura de rutas — todo lleva el tenant en el path

Todas las llamadas de negocio van prefijadas por el tenant:

```
http://localhost:8080/{tenant}/api/...
```

Donde `{tenant}` es el identificador de la empresa logueada, por ejemplo
`empresa1` o `empresa2`. El frontend necesita **guardar el tenant** (lo
recibe en la respuesta de login) y usarlo en cada llamada posterior.

## 4. Flujo de autenticación

### 4.1. Login

```http
POST http://localhost:8080/{tenant}/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Respuesta exitosa (200):**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "tenant": "empresa1"
}
```

**Qué debe hacer el frontend con esto:**
1. Guardar el `token` (en memoria/store de Svelte; **no usar localStorage**
   en producción sin evaluar el riesgo de XSS, pero para el alcance de este
   proyecto puede ser aceptable).
2. Guardar también el `tenant` recibido — se necesita para construir cada
   URL siguiente.
3. El token expira en `expiresIn` segundos (3600 = 1 hora). No hay endpoint
   de refresh documentado por ahora; al expirar, el usuario deberá loguearse
   de nuevo.

**Login fallido (401):**

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid username or password",
  "path": "/auth/login",
  "validationErrors": {}
}
```

### 4.2. Registro (si el frontend lo necesita)

```http
POST http://localhost:8080/{tenant}/api/auth/register
Content-Type: application/json

{
  "username": "nuevo_usuario",
  "password": "clave123"
}
```

Respuesta exitosa: **201 Created**.
Si el username ya existe en ese tenant: **409 Conflict**.

## 5. Llamadas autenticadas — Productos

Todas las rutas de productos requieren el header `Authorization` con el
token obtenido en el login:

```
Authorization: Bearer <token>
```

**El frontend NO necesita enviar `X-Tenant-Id` manualmente** — ese header lo
agrega el BFF internamente al hablar con Inventory Service. El frontend solo
necesita el tenant en el **path** de la URL.

### 5.1. Listar productos

```http
GET http://localhost:8080/{tenant}/api/products
Authorization: Bearer <token>
```

Respuesta (200):

```json
[
  {
    "id": 1,
    "sku": "SKU-TEST-001",
    "name": "Mouse Inalambrico Test",
    "description": "...",
    "category": "Accesorios",
    "unitPrice": 12990.00,
    "stock": 25,
    "active": true,
    "createdAt": "2026-06-19T01:48:57.422038Z",
    "updatedAt": "2026-06-19T01:48:57.422038Z"
  }
]
```

### 5.2. Obtener un producto por ID

```http
GET http://localhost:8080/{tenant}/api/products/{id}
Authorization: Bearer <token>
```

### 5.3. Crear producto

```http
POST http://localhost:8080/{tenant}/api/products
Authorization: Bearer <token>
Content-Type: application/json

{
  "sku": "SKU-001",
  "name": "Mouse Inalambrico",
  "description": "Mouse ergonómico",
  "category": "Accesorios",
  "unitPrice": 12990,
  "stock": 25
}
```

Respuesta exitosa: **201 Created**, con el producto creado (incluye `id`).

**Validaciones del body a tener en cuenta en el formulario del frontend:**

| Campo | Regla |
|---|---|
| `sku` | obligatorio, máx. 80 caracteres |
| `name` | obligatorio, máx. 160 caracteres |
| `description` | opcional, máx. 500 caracteres |
| `category` | obligatorio, máx. 100 caracteres |
| `unitPrice` | obligatorio, debe ser **mayor a 0** (no acepta 0 ni negativos) |
| `stock` | obligatorio, entero, **mínimo 0** |

Si el formulario manda algo fuera de estas reglas, el backend responde 400.

### 5.4. Actualizar producto completo

```http
PUT http://localhost:8080/{tenant}/api/products/{id}
Authorization: Bearer <token>
Content-Type: application/json

{ ... mismo formato que crear ... }
```

### 5.5. Actualizar solo el stock

```http
PATCH http://localhost:8080/{tenant}/api/products/{id}/stock
Authorization: Bearer <token>
Content-Type: application/json

{
  "stock": 40
}
```

### 5.6. Eliminar producto

```http
DELETE http://localhost:8080/{tenant}/api/products/{id}
Authorization: Bearer <token>
```

Respuesta exitosa: **204 No Content** (sin body).

## 6. Manejo de errores que el frontend debe contemplar

| Código | Cuándo ocurre | Qué mostrar en UI |
|---|---|---|
| 400 | Datos del formulario inválidos | Mensaje de validación (revisar `validationErrors` si viene) |
| 401 | Token ausente, expirado o inválido | Redirigir a login |
| 403 | El tenant del token no coincide con el de la URL — **no debería pasar en uso normal del frontend**, pero indica un bug si aparece (ej. token viejo de otro tenant guardado en el store) | Forzar logout y re-login |
| 404 | Producto no encontrado | Mensaje "producto no existe" |
| 409 | Conflicto (ej. username duplicado en registro) | Mensaje específico del campo |

El 403 en particular es la prueba de que el aislamiento multi-tenant
funciona — si el frontend cambia de tenant sin limpiar el token viejo,
el backend lo va a bloquear correctamente. Vale la pena que el store de
autenticación limpie el token completo al cambiar de tenant/cuenta.

## 7. Checklist rápido para la primera conexión

- [ ] Confirmar que CORS está habilitado en el BFF para el puerto de Vite
- [ ] Implementar el login y guardar `token` + `tenant` en el store
- [ ] Armar un wrapper de fetch que agregue automáticamente
      `Authorization: Bearer <token>` en cada llamada autenticada
- [ ] Construir las URLs con el tenant guardado: `` `${BFF_URL}/${tenant}/api/...` ``
- [ ] Probar el flujo: login → listar productos (vacío) → crear producto →
      listar de nuevo (debe aparecer)
- [ ] Verificar que al expirar o invalidar el token, la UI redirige a login
      en vez de mostrar pantallas rotas

## 8. Variable de entorno sugerida para el frontend

En vez de hardcodear `http://localhost:8080` en el código, usar una env var
de Vite:

```
# .env
VITE_BFF_URL=http://localhost:8080
```

```javascript
const BFF_URL = import.meta.env.VITE_BFF_URL;
```

Así, si más adelante se corre todo en Docker con nombres de servicio en vez
de `localhost`, solo se cambia esta variable.
