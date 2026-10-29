package com.ecommerce.tps.inventory;

import com.ecommerce.tps.api.ApiDtos.InventoryAdjustmentRequest;
import com.ecommerce.tps.api.ApiDtos.InventoryResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventario", description = "Consulta y ajustes de existencias por producto")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {
	private final InventoryService inventoryService;
	public InventoryController(InventoryService inventoryService) { this.inventoryService = inventoryService; }

	@GetMapping
	@Operation(summary = "Listar inventario", description = "Devuelve las existencias paginadas. Requiere rol ADMIN.")
	public Page<InventoryResponse> list(@ParameterObject @PageableDefault(size = 20) Pageable pageable) {
		return inventoryService.list(pageable);
	}

	@PostMapping("/{productId}/adjustments")
	@Operation(summary = "Ajustar existencias", description = "Suma o resta unidades al inventario indicando el motivo. El stock nunca puede quedar negativo. Requiere rol ADMIN.")
	public InventoryResponse adjust(@PathVariable long productId,
			@Valid @RequestBody InventoryAdjustmentRequest request) {
		return inventoryService.adjust(productId, request.delta(), request.reason());
	}
}
