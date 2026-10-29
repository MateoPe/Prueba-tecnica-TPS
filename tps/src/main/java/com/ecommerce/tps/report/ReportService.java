package com.ecommerce.tps.report;

import com.ecommerce.tps.api.ApiDtos.ProductResponse;
import com.ecommerce.tps.api.ApiDtos.TopCustomerResponse;
import com.ecommerce.tps.api.ApiDtos.TopProductResponse;
import com.ecommerce.tps.product.ProductService;
import com.ecommerce.tps.order.CustomerOrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReportService {
	private final ProductService productService;
	private final CustomerOrderRepository orders;

	public ReportService(ProductService productService, CustomerOrderRepository orders) {
		this.productService = productService;
		this.orders = orders;
	}

	@Transactional(readOnly = true)
	public Page<ProductResponse> activeProducts(PageRequest pageRequest) {
		return productService.search(true, null, null, pageRequest);
	}

	@Transactional(readOnly = true)
	public List<TopProductResponse> topSellingProducts() {
		return orders.topSellingProducts(PageRequest.of(0, 5)).stream()
				.map(row -> new TopProductResponse(((Number) row[0]).longValue(), (String) row[1],
						(String) row[2], ((Number) row[3]).longValue()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<TopCustomerResponse> topFrequentCustomers() {
		return orders.topFrequentCustomers(PageRequest.of(0, 5)).stream()
				.map(row -> new TopCustomerResponse(((Number) row[0]).longValue(), (String) row[1],
						(String) row[2], ((Number) row[3]).longValue()))
				.toList();
	}
}
