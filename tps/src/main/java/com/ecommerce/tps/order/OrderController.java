package com.ecommerce.tps.order;

import com.ecommerce.tps.api.ApiDtos.OrderRequest;
import com.ecommerce.tps.api.ApiDtos.OrderResponse;
import com.ecommerce.tps.api.ApiDtos.RandomOrderRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Órdenes", description = "Creación, consulta y cancelación de órdenes")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {
	private final OrderService orderService;
	public OrderController(OrderService orderService) { this.orderService = orderService; }

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear orden", description = "Crea una orden para el cliente autenticado y descuenta existencias. Aplica la política de descuentos configurada. Un administrador también puede crear una orden para su propia cuenta.")
	public OrderResponse place(@Valid @RequestBody OrderRequest request,
			@Parameter(hidden = true) Authentication authentication) {
		return orderService.place(authentication.getName(), request);
	}

	@PostMapping("/random")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear orden aleatoria", description = "Elige productos activos distintos con existencias disponibles y registra la orden. Dentro de la ventana configurada aplica el descuento aleatorio.")
	public OrderResponse placeRandom(@Valid @RequestBody RandomOrderRequest request,
			@Parameter(hidden = true) Authentication authentication) {
		return orderService.placeRandom(authentication.getName(), request.productCount());
	}

	@GetMapping
	@Operation(summary = "Listar órdenes", description = "El cliente ve sus propias órdenes; el administrador puede ver todas.")
	public Page<OrderResponse> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable,
			@Parameter(hidden = true) Authentication authentication) {
		return orderService.list(authentication.getName(), OrderService.isAdmin(authentication), pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar orden", description = "Solo el propietario de la orden o un administrador puede consultarla.")
	public OrderResponse get(@PathVariable long id, @Parameter(hidden = true) Authentication authentication) {
		return orderService.get(id, authentication.getName(), OrderService.isAdmin(authentication));
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancelar orden", description = "Cancela una orden completada y reintegra sus existencias. Solo el propietario o un administrador puede cancelarla.")
	public OrderResponse cancel(@PathVariable long id, @Parameter(hidden = true) Authentication authentication) {
		return orderService.cancel(id, authentication.getName(), OrderService.isAdmin(authentication));
	}
}
