package com.ecommerce.tps.order;

import com.ecommerce.tps.api.ApiDtos.OrderLineRequest;
import com.ecommerce.tps.api.ApiDtos.OrderRequest;
import com.ecommerce.tps.api.ApiDtos.OrderResponse;
import com.ecommerce.tps.audit.AuditService;
import com.ecommerce.tps.common.ApiException;
import com.ecommerce.tps.inventory.Inventory;
import com.ecommerce.tps.inventory.InventoryRepository;
import com.ecommerce.tps.product.Product;
import com.ecommerce.tps.product.ProductRepository;
import com.ecommerce.tps.user.UserAccount;
import com.ecommerce.tps.user.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
	private final CustomerOrderRepository orders;
	private final UserAccountRepository users;
	private final ProductRepository products;
	private final InventoryRepository inventory;
	private final DiscountPolicy discountPolicy;
	private final AuditService auditService;

	public OrderService(CustomerOrderRepository orders, UserAccountRepository users, ProductRepository products,
			InventoryRepository inventory, DiscountPolicy discountPolicy, AuditService auditService) {
		this.orders = orders;
		this.users = users;
		this.products = products;
		this.inventory = inventory;
		this.discountPolicy = discountPolicy;
		this.auditService = auditService;
	}

	@Transactional
	public OrderResponse place(String email, OrderRequest request) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		for (OrderLineRequest item : request.items()) {
			try {
				quantities.merge(item.productId(), item.quantity(), Math::addExact);
			} catch (ArithmeticException exception) {
				throw ApiException.badRequest("La cantidad solicitada es demasiado grande");
			}
		}
		return createOrder(email, quantities, false);
	}

	@Transactional
	public OrderResponse placeRandom(String email, int productCount) {
		List<Product> availableProducts = new ArrayList<>(products.findAll().stream()
				.filter(Product::isActive)
				.filter(product -> inventory.findByProductId(product.getId())
						.map(stock -> stock.getQuantity() > 0).orElse(false))
				.toList());
		Collections.shuffle(availableProducts);
		if (availableProducts.size() < productCount) {
			throw ApiException.badRequest("No hay suficientes productos activos con inventario para el pedido aleatorio");
		}
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		availableProducts.stream().limit(productCount).forEach(product -> quantities.put(product.getId(), 1));
		return createOrder(email, quantities, true);
	}

	private OrderResponse createOrder(String email, Map<Long, Integer> quantities, boolean randomOrder) {
		UserAccount customer = users.findByEmailIgnoreCase(email).filter(UserAccount::isActive)
				.orElseThrow(() -> ApiException.unauthorized("La cuenta no está disponible"));
		List<Long> productIds = quantities.keySet().stream().sorted().toList();
		List<Inventory> lockedStock = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO;
		for (Long productId : productIds) {
			int quantity = quantities.get(productId);
			Product product = products.findById(productId)
					.filter(Product::isActive)
					.orElseThrow(() -> ApiException.notFound("Producto no encontrado o inactivo: " + productId));
			Inventory stock = inventory.findByProductIdForUpdate(productId)
					.orElseThrow(() -> ApiException.conflict("El producto no tiene inventario configurado"));
			if (stock.getQuantity() < quantity) {
				throw ApiException.badRequest("Inventario insuficiente para el producto " + product.getSku());
			}
			lockedStock.add(stock);
			subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
		}
		long completedOrders = orders.countCompletedByCustomer(customer.getId(), OrderStatus.COMPLETED);
		boolean frequentCustomer = completedOrders >= discountPolicy.getFrequentOrderThreshold();
		Instant now = Instant.now();
		BigDecimal discountPercent = discountPolicy.percent(randomOrder, frequentCustomer, now);
		BigDecimal total = subtotal.multiply(BigDecimal.ONE.subtract(
				discountPercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)))
				.setScale(2, RoundingMode.HALF_UP);

		CustomerOrder order = new CustomerOrder(customer, subtotal.setScale(2, RoundingMode.HALF_UP),
				discountPercent, total, randomOrder);
		for (int index = 0; index < productIds.size(); index++) {
			Long productId = productIds.get(index);
			Product product = products.findById(productId).orElseThrow();
			int quantity = quantities.get(productId);
			lockedStock.get(index).subtract(quantity);
			order.addItem(new OrderItem(product, quantity, product.getPrice()));
		}
		CustomerOrder saved = orders.save(order);
		auditService.record("CREATE", "CustomerOrder", saved.getId(),
				(randomOrder ? "Pedido aleatorio; " : "Pedido; ") + "total: " + total);
		return OrderResponse.from(saved);
	}

	@Transactional(readOnly = true)
	public Page<OrderResponse> list(String email, boolean admin, Pageable pageable) {
		Page<CustomerOrder> page = admin ? orders.findAll(pageable)
				: orders.findByCustomerIdOrderByCreatedAtDesc(customerId(email), pageable);
		return page.map(OrderResponse::from);
	}

	@Transactional(readOnly = true)
	public OrderResponse get(long id, String email, boolean admin) {
		CustomerOrder order = orders.findById(id).orElseThrow(() -> ApiException.notFound("Pedido no encontrado"));
		if (!admin && !order.getCustomer().getEmail().equalsIgnoreCase(email)) {
			throw ApiException.notFound("Pedido no encontrado");
		}
		return OrderResponse.from(order);
	}

	@Transactional
	public OrderResponse cancel(long id, String email, boolean admin) {
		CustomerOrder order = orders.findByIdForUpdate(id)
				.orElseThrow(() -> ApiException.notFound("Pedido no encontrado"));
		if (!admin && !order.getCustomer().getEmail().equalsIgnoreCase(email)) {
			throw ApiException.notFound("Pedido no encontrado");
		}
		if (order.getStatus() != OrderStatus.COMPLETED) {
			throw ApiException.conflict("El pedido ya fue cancelado");
		}
		for (OrderItem item : order.getItems()) {
			Inventory stock = inventory.findByProductIdForUpdate(item.getProduct().getId())
					.orElseThrow(() -> ApiException.conflict("El producto no tiene inventario configurado"));
			stock.add(item.getQuantity());
		}
		order.cancel();
		auditService.record("CANCEL", "CustomerOrder", id, "Pedido cancelado; inventario reintegrado");
		return OrderResponse.from(order);
	}

	private long customerId(String email) {
		return users.findByEmailIgnoreCase(email).map(UserAccount::getId)
				.orElseThrow(() -> ApiException.unauthorized("La cuenta no está disponible"));
	}

	public static boolean isAdmin(Authentication authentication) {
		return authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
	}
}
