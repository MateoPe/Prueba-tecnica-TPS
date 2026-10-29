# Nexo Shop — Frontend

Frontend de e-commerce construido con Angular y TypeScript, integrado con la API Spring Boot. Incluye autenticación, registro de clientes, catálogo, inventario, pedidos, usuarios, reportes y auditoría.

## Requisitos

- Node.js compatible con Angular 21
- npm
- Backend disponible en `http://localhost:8080`

## Ejecutar en desarrollo

```bash
npm install
npm start
```

Abre `http://localhost:4200/`.

`npm start` carga `proxy.conf.json`: las solicitudes del navegador a `/api/**` se reenvían a `http://localhost:8080`. El proxy usa el origen del backend para que Spring no rechace los POST si el frontend utiliza otro puerto local. Si cambias el puerto o host del backend, actualiza `target` y `headers.Origin` en ese archivo y reinicia el servidor de desarrollo.

Documentación del backend: `http://localhost:8080/swagger-ui.html`; contrato OpenAPI: `http://localhost:8080/v3/api-docs`.

## Autenticación y permisos

- Registro mediante `POST /api/auth/register`. La API crea una cuenta `CUSTOMER`; después se inicia sesión.
- Login mediante `POST /api/auth/login`. El interceptor agrega `Authorization: Bearer <accessToken>` únicamente a solicitudes de la API.
- El token permanece **solo en memoria**: al recargar o cerrar la pestaña se debe iniciar sesión nuevamente. No se guarda la contraseña ni el token en almacenamiento del navegador. La sesión se cierra al vencer `expiresIn` o recibir un `401` autenticado.
- Los clientes pueden consultar productos activos y gestionar sus propios pedidos. Los administradores también gestionan productos, inventario y usuarios, consultan todas las órdenes y acceden a reportes/auditoría.
- El frontend oculta acciones según el rol; la autorización real corresponde al backend.

## Funcionalidades integradas

| Área       | Comportamiento                                                                                                                                                       |
| ---------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Productos  | Búsqueda por nombre/SKU/descripción, categoría exacta, estado activo/inactivo para ADMIN, ordenamiento y paginación del servidor. Creación, edición y desactivación. |
| Inventario | Listado paginado y ajustes con `delta` entero distinto de cero y motivo obligatorio. Editar un producto no modifica su stock.                                        |
| Pedidos    | Carrito con varios productos, pedido aleatorio de 1 a 10 productos, listado paginado, detalle y cancelación con reintegro de stock por el servidor.                  |
| Usuarios   | Listado, edición de nombre/rol/estado, cambio opcional de contraseña y desactivación. No hay invitaciones ni roles fuera de ADMIN/CUSTOMER en este contrato.         |
| Reportes   | Productos activos paginados, top 5 por unidades vendidas y top 5 clientes por órdenes completadas.                                                                   |
| Auditoría  | Registros paginados de 50 en 50, ordenados por fecha descendente.                                                                                                    |

Las órdenes se crean para la cuenta del token; no se puede seleccionar otro cliente. El backend selecciona los productos aleatorios, valida el inventario y calcula `subtotal`, `discountPercent` y `total`; el frontend muestra la respuesta sin recalcular descuentos. El rango de promociones se configura en el backend: el contrato no ofrece endpoints para modificarlo.

No hay datos simulados ni variaciones/gráficas de ventas inventadas. El resumen identifica explícitamente el importe de los últimos cinco pedidos, no un total histórico. Las fechas ISO UTC se muestran en la zona horaria del navegador. Los errores de la API, incluidos conflictos, stock insuficiente y falta de permisos, se muestran en pantalla sin convertirlos en resultados exitosos.

## Despliegue

El build se genera en `dist/frontend/browser`. En producción, el servidor web debe reenviar `/api/**` al backend; el proxy del CLI solo funciona en desarrollo. Como alternativa, se puede proporcionar `API_BASE_URL` en la configuración de Angular con una URL absoluta y configurar CORS en Spring Boot para el origen del frontend. Usa HTTPS en producción.

## Validación

```bash
npm test -- --watch=false
npm run build
```

Las pruebas usan `HttpTestingController` y no modifican la base de datos. Cubren autenticación y expiración, roles, contratos HTTP, filtros y paginación, errores, productos, pedidos normales/aleatorios y cancelación.
