# Contrato de API para frontend

## Conexión y autenticación

- URL local: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Requests y responses usan `Content-Type: application/json`.
- Para endpoints autenticados enviar `Authorization: Bearer <accessToken>`.
- Registra un cliente o inicia sesión para obtener `accessToken`. Las rutas de administración requieren un usuario con rol `ADMIN`.
- Los valores `id` son numéricos. Fechas se devuelven en ISO-8601 UTC.

## Modelos compartidos

### Usuario (`UserResponse`)

```json
{
  "id": 1,
  "email": "cliente@example.com",
  "fullName": "Ana Pérez",
  "role": "CUSTOMER",
  "active": true,
  "createdAt": "2026-10-29T12:00:00Z",
  "updatedAt": "2026-10-29T12:00:00Z"
}
```

`role` puede ser `CUSTOMER` o `ADMIN`. La respuesta nunca incluye la contraseña.

### Producto (`ProductResponse`)

```json
{
  "id": 1,
  "sku": "SKU-001",
  "name": "Camiseta TPS",
  "description": "Camiseta talla M",
  "category": "Ropa",
  "price": 49900.00,
  "active": true,
  "stock": 25,
  "createdAt": "2026-10-29T12:00:00Z",
  "updatedAt": "2026-10-29T12:00:00Z"
}
```

### Orden (`OrderResponse`)

```json
{
  "id": 1,
  "customerId": 2,
  "customerEmail": "cliente@example.com",
  "items": [
    {
      "productId": 1,
      "productName": "Camiseta TPS",
      "productSku": "SKU-001",
      "quantity": 2,
      "unitPrice": 49900.00,
      "lineTotal": 99800.00
    }
  ],
  "subtotal": 99800.00,
  "discountPercent": 10.00,
  "total": 89820.00,
  "randomOrder": false,
  "status": "COMPLETED",
  "createdAt": "2026-10-29T12:00:00Z"
}
```

`status` puede ser `COMPLETED` o `CANCELLED`. `discountPercent` es el porcentaje total aplicado; el descuento frecuente se suma en puntos porcentuales cuando corresponda.

### Respuesta paginada

Los endpoints paginados devuelven la página dentro de `content`, junto con metadatos de Spring:

```json
{
  "content": [],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "empty": true, "sorted": false, "unsorted": true },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalElements": 0,
  "totalPages": 0,
  "last": true,
  "size": 20,
  "number": 0,
  "sort": { "empty": true, "sorted": false, "unsorted": true },
  "numberOfElements": 0,
  "first": true,
  "empty": true
}
```

Los parámetros de página son `page` (empieza en `0`) y `size`. En endpoints que aceptan orden también se admite `sort`, por ejemplo `sort=name,asc`. El tamaño predeterminado es `20`, excepto auditoría (`50`). El reporte de productos activos siempre ordena por nombre ascendente.

### Error (`ApiError`)

```json
{
  "timestamp": "2026-10-29T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "El motivo del error",
  "path": "/api/orders"
}
```

Errores comunes: `400` datos inválidos o stock insuficiente, `401` token/credenciales ausentes o inválidos, `403` permisos insuficientes, `404` recurso inexistente, `409` correo/SKU duplicado o conflicto con el estado del recurso.

## Autenticación

### `POST /api/auth/register` — Público

Registra un cliente. El rol siempre se crea como `CUSTOMER`.

Request:

```json
{
  "email": "cliente@example.com",
  "fullName": "Ana Pérez",
  "password": "Cliente123!"
}
```

`email` debe ser válido y único; `fullName` es obligatorio (máximo 100 caracteres); `password` debe tener entre 8 y 72 caracteres.

Respuesta `201 Created` (`UserResponse`):

```json
{
  "id": 2,
  "email": "cliente@example.com",
  "fullName": "Ana Pérez",
  "role": "CUSTOMER",
  "active": true,
  "createdAt": "2026-10-29T12:00:00Z",
  "updatedAt": "2026-10-29T12:00:00Z"
}
```

### `POST /api/auth/login` — Público

Request:

```json
{
  "email": "cliente@example.com",
  "password": "Cliente123!"
}
```

Respuesta `200 OK`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 2,
    "email": "cliente@example.com",
    "fullName": "Ana Pérez",
    "role": "CUSTOMER",
    "active": true,
    "createdAt": "2026-10-29T12:00:00Z",
    "updatedAt": "2026-10-29T12:00:00Z"
  }
}
```

Para requests siguientes: `Authorization: Bearer <accessToken>`.

## Productos

### `GET /api/products` — Público para activos

Query params opcionales:

- `q`: busca parcialmente en nombre, SKU o descripción.
- `category`: coincidencia exacta de categoría, sin distinguir mayúsculas.
- `active`: por defecto `true`. `active=false` requiere `ADMIN`.
- `page`, `size`, `sort`: paginación.

Ejemplo: `GET /api/products?q=camiseta&category=Ropa&page=0&size=10`

Respuesta `200 OK`: página de `ProductResponse`.

### `GET /api/products/{id}` — Público

Ejemplo: `GET /api/products/1`

Respuesta `200 OK`: `ProductResponse`. Los productos inactivos no se devuelven.

### `POST /api/products` — `ADMIN`

Request:

```json
{
  "sku": "SKU-001",
  "name": "Camiseta TPS",
  "description": "Camiseta talla M",
  "category": "Ropa",
  "price": 49900.00,
  "active": true,
  "initialStock": 25
}
```

`price` debe ser positivo, hasta dos decimales; `initialStock` debe ser un entero no negativo.

Respuesta `201 Created`: `ProductResponse`.

### `PUT /api/products/{id}` — `ADMIN`

Request:

```json
{
  "sku": "SKU-001",
  "name": "Camiseta TPS edición nueva",
  "description": "Camiseta talla M",
  "category": "Ropa",
  "price": 52900.00,
  "active": true
}
```

Respuesta `200 OK`: `ProductResponse`. No modifica el inventario.

### `DELETE /api/products/{id}` — `ADMIN`

Desactiva el producto; no borra el historial ni su inventario.

Respuesta `204 No Content` (sin body).

## Inventario — `ADMIN`

### `GET /api/inventory`

Query params: `page`, `size`, `sort`.

Respuesta `200 OK`: página de:

```json
{
  "productId": 1,
  "sku": "SKU-001",
  "productName": "Camiseta TPS",
  "quantity": 25,
  "updatedAt": "2026-10-29T12:00:00Z"
}
```

### `POST /api/inventory/{productId}/adjustments`

Request:

```json
{
  "delta": 5,
  "reason": "Recepción de compra"
}
```

`delta` no puede ser cero; un valor negativo resta existencias, sin permitir stock negativo.

Respuesta `200 OK`: `InventoryResponse` (mismo esquema del listado).

## Órdenes — usuario autenticado

### `POST /api/orders`

Crea una orden para la cuenta del token.

Request:

```json
{
  "items": [
    { "productId": 1, "quantity": 2 },
    { "productId": 3, "quantity": 1 }
  ]
}
```

Cada `productId` es obligatorio y cada `quantity` debe ser al menos `1`. Si un producto se repite en la lista, las cantidades se suman.

Respuesta `201 Created`: `OrderResponse`.

### `POST /api/orders/random`

El backend elige productos activos distintos que tengan stock.

Request:

```json
{
  "productCount": 2
}
```

`productCount` debe estar entre `1` y `10`; si no hay suficientes productos disponibles responde `400`.

Respuesta `201 Created`: `OrderResponse` con `randomOrder: true`.

### `GET /api/orders`

Query params: `page`, `size`, `sort`.

El cliente recibe sus propias órdenes; el administrador recibe todas.

Respuesta `200 OK`: página de `OrderResponse`.

### `GET /api/orders/{id}`

Respuesta `200 OK`: `OrderResponse`. Solo puede consultarla su propietario o un administrador. Para un pedido ajeno devuelve `404`.

### `POST /api/orders/{id}/cancel`

Cancela una orden completada y reintegra el stock.

Respuesta `200 OK`: `OrderResponse` con `status: "CANCELLED"`. Una orden ya cancelada devuelve `409`.

## Usuarios — `ADMIN`

### `GET /api/users`

Query params: `page`, `size`, `sort`.

Respuesta `200 OK`: página de `UserResponse`.

### `GET /api/users/{id}`

Respuesta `200 OK`: `UserResponse`.

### `PUT /api/users/{id}`

Request:

```json
{
  "fullName": "Ana Pérez",
  "role": "CUSTOMER",
  "active": true,
  "password": null
}
```

`role` es `CUSTOMER` o `ADMIN`. Para conservar la contraseña existente, enviar `null` u omitir `password`; si se cambia, debe tener entre 8 y 72 caracteres.

Respuesta `200 OK`: `UserResponse`.

### `DELETE /api/users/{id}`

Desactiva la cuenta; no la elimina ni borra las órdenes asociadas.

Respuesta `204 No Content` (sin body).

## Reportes — `ADMIN`

### `GET /api/reports/products/active`

Query params: `page` (predeterminado `0`), `size` (predeterminado `20`).

Respuesta `200 OK`: página de `ProductResponse`, ordenada por nombre ascendente.

### `GET /api/reports/products/top-selling`

Sin request body ni query params.

Respuesta `200 OK`:

```json
[
  {
    "productId": 1,
    "name": "Camiseta TPS",
    "sku": "SKU-001",
    "unitsSold": 42
  }
]
```

Devuelve hasta cinco productos, contados por unidades en órdenes completadas.

### `GET /api/reports/customers/frequent`

Sin request body ni query params.

Respuesta `200 OK`:

```json
[
  {
    "customerId": 2,
    "fullName": "Ana Pérez",
    "email": "cliente@example.com",
    "completedOrders": 7
  }
]
```

Devuelve hasta cinco clientes ordenados por número de órdenes completadas.

## Auditoría — `ADMIN`

### `GET /api/audit`

Query params: `page`, `size` (predeterminado `50`), `sort`.

Respuesta `200 OK`: página de:

```json
{
  "id": 1,
  "actorEmail": "admin@tps.local",
  "action": "CREATE",
  "entityType": "Product",
  "entityId": "1",
  "details": "Producto creado",
  "occurredAt": "2026-10-29T12:00:00Z"
}
```

