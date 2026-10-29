package com.ecommerce.tps.product;

import com.ecommerce.tps.api.ApiDtos.ProductRequest;
import com.ecommerce.tps.api.ApiDtos.ProductResponse;
import com.ecommerce.tps.api.ApiDtos.ProductUpdateRequest;
import com.ecommerce.tps.common.ApiException;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Productos", description = "Búsqueda pública y administración del catálogo")
public class ProductController {
	private final ProductService productService;
	public ProductController(ProductService productService) { this.productService = productService; }

	@GetMapping
	@Operation(summary = "Buscar productos", description = "Busca por nombre, SKU o descripción, y filtra opcionalmente por categoría y estado. Solo un administrador puede incluir productos inactivos.")
	public Page<ProductResponse> search(
			@RequestParam(required = false, defaultValue = "true") Boolean active,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String category,
			@ParameterObject @PageableDefault(size = 20) Pageable pageable,
			@Parameter(hidden = true) Authentication authentication) {
		if (!Boolean.TRUE.equals(active) && (authentication == null
				|| authentication.getAuthorities().stream()
						.noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN")))) {
			throw ApiException.forbidden("Solo administradores pueden consultar productos inactivos");
		}
		return productService.search(active, q, category, pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Consultar producto", description = "Devuelve los detalles y las existencias de un producto activo.")
	public ProductResponse get(@PathVariable long id) { return productService.get(id); }

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Crear producto", description = "Crea un producto y su inventario inicial. Requiere rol ADMIN.",
			security = @SecurityRequirement(name = "bearerAuth"))
	public ProductResponse create(@Valid @RequestBody ProductRequest request) {
		return productService.create(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Actualizar producto", description = "Actualiza los datos del producto sin modificar su inventario. Requiere rol ADMIN.",
			security = @SecurityRequirement(name = "bearerAuth"))
	public ProductResponse update(@PathVariable long id, @Valid @RequestBody ProductUpdateRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(summary = "Desactivar producto", description = "Desactiva el producto sin borrar el historial de órdenes. Requiere rol ADMIN.",
			security = @SecurityRequirement(name = "bearerAuth"))
	public void deactivate(@PathVariable long id) { productService.deactivate(id); }
}
