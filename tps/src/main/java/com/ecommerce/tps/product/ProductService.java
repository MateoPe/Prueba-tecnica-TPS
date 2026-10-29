package com.ecommerce.tps.product;

import com.ecommerce.tps.api.ApiDtos.ProductRequest;
import com.ecommerce.tps.api.ApiDtos.ProductResponse;
import com.ecommerce.tps.api.ApiDtos.ProductUpdateRequest;
import com.ecommerce.tps.audit.AuditService;
import com.ecommerce.tps.common.ApiException;
import com.ecommerce.tps.inventory.Inventory;
import com.ecommerce.tps.inventory.InventoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
	private final ProductRepository products;
	private final InventoryRepository inventory;
	private final AuditService auditService;

	public ProductService(ProductRepository products, InventoryRepository inventory, AuditService auditService) {
		this.products = products;
		this.inventory = inventory;
		this.auditService = auditService;
	}

	@Transactional(readOnly = true)
	public Page<ProductResponse> search(Boolean active, String term, String category, Pageable pageable) {
		String cleanedTerm = term == null ? "" : term.trim();
		String cleanedCategory = category == null ? "" : category.trim();
		return products.search(active, cleanedTerm, cleanedCategory, pageable)
				.map(product -> ProductResponse.from(product, inventory.findByProductId(product.getId()).orElse(null)));
	}

	@Transactional(readOnly = true)
	public ProductResponse get(long id) {
		Product product = find(id);
		if (!product.isActive()) {
			throw ApiException.notFound("Producto no encontrado");
		}
		return ProductResponse.from(product, inventory.findByProductId(id).orElse(null));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		Product product = products.save(new Product(request.sku().trim(), request.name().trim(),
				request.description(), request.category().trim(), request.price(), request.active()));
		Inventory stock = inventory.save(new Inventory(product, request.initialStock()));
		auditService.record("CREATE", "Product", product.getId(), "Producto creado");
		return ProductResponse.from(product, stock);
	}

	@Transactional
	public ProductResponse update(long id, ProductUpdateRequest request) {
		Product product = find(id);
		product.update(request.sku().trim(), request.name().trim(), request.description(),
				request.category().trim(), request.price(), request.active());
		auditService.record("UPDATE", "Product", product.getId(), "Producto actualizado");
		return ProductResponse.from(product, inventory.findByProductId(id).orElse(null));
	}

	@Transactional
	public void deactivate(long id) {
		Product product = find(id);
		product.update(product.getSku(), product.getName(), product.getDescription(), product.getCategory(),
				product.getPrice(), false);
		auditService.record("DEACTIVATE", "Product", product.getId(), "Producto desactivado");
	}

	Product find(long id) {
		return products.findById(id).orElseThrow(() -> ApiException.notFound("Producto no encontrado"));
	}
}
