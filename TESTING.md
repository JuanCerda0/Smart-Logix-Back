# SmartLogix Backend Testing

## Objetivo

Las pruebas del backend verifican que los tres servicios mantengan sus contratos principales:

- `BackendForFrontend`: frontera publica para frontend, validacion JWT, tenant del path, autorizacion cruzada y reenvio de requests hacia Auth e Inventory.
- `auth-service`: login, registro, validacion de tenant y emision de JWT con claims esperados.
- `inventory-service`: API de productos, aislamiento por tenant, validaciones de dominio y errores HTTP.

El BFF tiene mayor peso porque concentra la superficie publica del sistema. Si falla ahi, el frontend puede llamar a rutas equivocadas, saltarse validaciones de tenant o perder headers criticos antes de llegar a los microservicios internos.

## Linea Base

Antes de ampliar la suite, la linea base registrada era:

| Servicio | Resultado base |
| --- | --- |
| BackendForFrontend | 6/6 tests verdes |
| auth-service | 5/5 tests verdes |
| inventory-service | 7/7 tests verdes |

Tras los refuerzos, los resultados verificados son:

| Servicio | Resultado actual |
| --- | --- |
| BackendForFrontend | 22/22 tests verdes |
| auth-service | 9/9 tests verdes |
| inventory-service | 12/12 tests verdes |

## Cobertura BFF

La meta practica para `BackendForFrontend` era al menos 70% con JaCoCo. La ejecucion actual registra:

| Metrica | Valor |
| --- | --- |
| Instruction coverage | 96.32% |
| Instrucciones cubiertas | 786 / 816 |
| Reporte | `BackendForFrontend/target/site/jacoco/index.html` |

## Casos Cubiertos

| Servicio | Clase de test | Casos principales | Resultado esperado |
| --- | --- | --- | --- |
| BackendForFrontend | `ProductControllerSecurityTests` | JWT requerido en todos los endpoints protegidos, tenant del token igual al tenant del path, rechazo de tenant cruzado, reenvio de `Authorization`, validaciones `400`, propagacion downstream `401/403/404/409` | Seguridad y contrato HTTP del BFF preservados |
| BackendForFrontend | `InventoryClientTests` | Rutas internas `/products`, `/products/{id}`, `/products/{id}/stock`, metodos HTTP, `X-Tenant-Id`, `Authorization` y body enviado | Cliente Inventory llama al contrato interno correcto |
| BackendForFrontend | `AuthServiceTests` | Login/register hacia `/auth/login` y `/auth/register`, header `X-Tenant-Id` y body enviado | Cliente Auth llama al contrato interno correcto |
| BackendForFrontend | `AuthControllerTests` | Login y registro desde rutas publicas por tenant | Respuestas del Auth interno se exponen al frontend |
| auth-service | `AuthControllerTests` | Login seed admin, password invalida, registro, usuario duplicado, tenant inexistente, header tenant faltante, request invalido y claim `tenant` en JWT | Auth valida identidad, tenant y formato de request |
| inventory-service | `ProductControllerTests` | Autenticacion, header tenant, creacion/listado, stock, aislamiento, SKU duplicado, producto inexistente, update completo, delete, producto invalido y stock negativo | Inventory protege y valida operaciones de producto por tenant |

## Comandos

Ejecutar desde cada carpeta de servicio:

```powershell
cd Smart-Logix-Back\BackendForFrontend
mvn test

cd ..\auth-service
mvn test

cd ..\inventory-service
mvn test
```

Cada servicio genera su reporte JaCoCo en:

```text
target/site/jacoco/index.html
```

## Nota De Entorno

En la sesion de trabajo se uso Maven global con `mvn test`. El plan original indicaba que `mvnw.cmd` habia fallado previamente, por lo que no se uso como comando principal de verificacion.
