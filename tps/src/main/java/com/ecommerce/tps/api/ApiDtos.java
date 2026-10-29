package com.ecommerce.tps.api;

import com.ecommerce.tps.audit.AuditLog;
import com.ecommerce.tps.inventory.Inventory;
import com.ecommerce.tps.order.CustomerOrder;
import com.ecommerce.tps.order.OrderItem;
import com.ecommerce.tps.product.Product;
import com.ecommerce.tps.user.UserAccount;
import com.ecommerce.tps.user.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(name = "Modelos de la API", description = "Modelos de solicitud y respuesta para la API de TPS e-commerce")
public final class ApiDtos {
	private ApiDtos() {
	}

	@Schema(description = "Datos para registrar una cuenta de cliente")
	public record RegisterRequest(
			@Schema(description = "Correo único del cliente", example = "cliente@example.com")
			@NotBlank @Email @Size(max = 254) String email,
			@Schema(description = "Nombre completo", example = "Ana Pérez")
			@NotBlank @Size(max = 100) String fullName,
			@Schema(description = "Contraseña de 8 a 72 caracteres", example = "Cliente123!")
			@NotBlank @Size(min = 8, max = 72) String password) {
	}

	@Schema(description = "Credenciales para iniciar sesión")
	public record LoginRequest(
			@Schema(example = "admin@tps.local") @NotBlank @Email String email,
			@Schema(example = "Admin123456!") @NotBlank String password) {
	}

	@Schema(description = "Perfil público; nunca contiene la contraseña")
	public record UserResponse(Long id, String email, String fullName, UserRole role, boolean active,
			Instant createdAt, Instant updatedAt) {
		public static UserResponse from(UserAccount user) {
			return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
					user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
		}
	}

	@Schema(description = "Token JWT y perfil autenticado")
	public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
	}

	@Schema(description = "Campos editables de la cuenta; password es opcional")
	public record UserUpdateRequest(
			@NotBlank @Size(max = 100) String fullName,
			@NotNull UserRole role,
			@NotNull Boolean active,
			@Size(min = 8, max = 72) String password) {
	}

	@Schema(description = "Datos para crear producto e inventario inicial")
	public record ProductRequest(
			@Schema(example = "SKU-001")
			@NotBlank @Size(max = 80) String sku,
			@Schema(example = "Camiseta TPS")
			@NotBlank @Size(max = 160) String name,
			@Size(max = 2000) String description,
			@Schema(example = "Ropa")
			@NotBlank @Size(max = 100) String category,
			@Schema(example = "49900.00")
			@NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
			@NotNull Boolean active,
			@Schema(description = "Unidades iniciales en existencia", example = "25")
			@NotNull @Min(0) Integer initialStock) {
	}

	@Schema(description = "Datos para actualizar un producto")
	public record ProductUpdateRequest(
			@NotBlank @Size(max = 80) String sku,
			@NotBlank @Size(max = 160) String name,
			@Size(max = 2000) String description,
			@NotBlank @Size(max = 100) String category,
			@NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
			@NotNull Boolean active) {
	}

	@Schema(description = "Datos públicos del producto y existencias actuales")
	public record ProductResponse(Long id, String sku, String name, String description, String category,
			BigDecimal price, boolean active, int stock, Instant createdAt, Instant updatedAt) {
		public static ProductResponse from(Product product, Inventory inventory) {
			return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getDescription(),
					product.getCategory(), product.getPrice(), product.isActive(),
					inventory == null ? 0 : inventory.getQuantity(), product.getCreatedAt(), product.getUpdatedAt());
		}
	}

	@Schema(description = "Ajuste de existencias; delta negativo resta unidades")
	public record InventoryAdjustmentRequest(
			@Schema(description = "Unidades a sumar o restar; no puede ser cero", example = "5")
			@NotNull Integer delta,
			@Schema(description = "Motivo del ajuste", example = "Recepción de compra")
			@NotBlank @Size(max = 500) String reason) {
	}

	@Schema(description = "Existencias de un producto")
	public record InventoryResponse(Long productId, String sku, String productName, int quantity, Instant updatedAt) {
		public static InventoryResponse from(Inventory inventory) {
			Product product = inventory.getProduct();
			return new InventoryResponse(product.getId(), product.getSku(), product.getName(),
					inventory.getQuantity(), inventory.getUpdatedAt());
		}
	}

	@Schema(description = "Producto y cantidad solicitada")
	public record OrderLineRequest(
			@Schema(example = "1") @NotNull Long productId,
			@Schema(example = "2") @NotNull @Min(1) Integer quantity) {
	}

	@Schema(description = "Productos que conforman la orden")
	public record OrderRequest(@NotEmpty List<@Valid OrderLineRequest> items) {
	}

	@Schema(description = "Cantidad de productos distintos a seleccionar aleatoriamente")
	public record RandomOrderRequest(@NotNull @Min(1) @jakarta.validation.constraints.Max(10) Integer productCount) {
	}

	@Schema(description = "Línea de una orden con precio histórico")
	public record OrderItemResponse(Long productId, String productName, String productSku, int quantity,
			BigDecimal unitPrice, BigDecimal lineTotal) {
		public static OrderItemResponse from(OrderItem item) {
			return new OrderItemResponse(item.getProduct().getId(), item.getProductName(), item.getProductSku(),
					item.getQuantity(), item.getUnitPrice(), item.getLineTotal());
		}
	}

	@Schema(description = "Orden con totales, descuento y líneas")
	public record OrderResponse(Long id, Long customerId, String customerEmail, List<OrderItemResponse> items,
			BigDecimal subtotal, BigDecimal discountPercent, BigDecimal total, boolean randomOrder, String status,
			Instant createdAt) {
		public static OrderResponse from(CustomerOrder order) {
			return new OrderResponse(order.getId(), order.getCustomer().getId(), order.getCustomer().getEmail(),
					order.getItems().stream().map(OrderItemResponse::from).toList(), order.getSubtotal(),
					order.getDiscountPercent(), order.getTotal(), order.isRandomOrder(), order.getStatus().name(),
					order.getCreatedAt());
		}
	}

	public record TopProductResponse(Long productId, String name, String sku, long unitsSold) {
	}

	public record TopCustomerResponse(Long customerId, String fullName, String email, long completedOrders) {
	}

	public record AuditLogResponse(Long id, String actorEmail, String action, String entityType, String entityId,
			String details, Instant occurredAt) {
		public static AuditLogResponse from(AuditLog log) {
			return new AuditLogResponse(log.getId(), log.getActorEmail(), log.getAction(), log.getEntityType(),
					log.getEntityId(), log.getDetails(), log.getOccurredAt());
		}
	}

	public record ApiError(Instant timestamp, int status, String error, String message, String path) {
	}
}
