package com.ecommerce.tps.inventory;

import com.ecommerce.tps.api.ApiDtos.InventoryResponse;
import com.ecommerce.tps.audit.AuditService;
import com.ecommerce.tps.common.ApiException;
import com.ecommerce.tps.product.Product;
import com.ecommerce.tps.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
	private final InventoryRepository inventory;
	private final ProductRepository products;
	private final AuditService auditService;

	public InventoryService(InventoryRepository inventory, ProductRepository products, AuditService auditService) {
		this.inventory = inventory;
		this.products = products;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public Page<InventoryResponse> list(Pageable pageable) {
		return inventory.findAll(pageable).map(InventoryResponse::from);
	}

	@Transactional
	public InventoryResponse adjust(long productId, int delta, String reason) {
		if (delta == 0) {
			throw ApiException.badRequest("El ajuste de inventario no puede ser cero");
		}
		Product product = products.findById(productId)
				.orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
		Inventory stock = inventory.findByProductIdForUpdate(productId)
				.orElseThrow(() -> ApiException.notFound("Inventario no encontrado"));
		if ((long) stock.getQuantity() + delta < 0) {
			throw ApiException.badRequest("El ajuste no puede dejar inventario negativo");
		}
		stock.add(delta);
		auditService.record("ADJUST", "Inventory", productId,
				"Variación: " + delta + "; motivo: " + reason.trim());
		return InventoryResponse.from(stock);
	}
}
