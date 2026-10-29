package com.ecommerce.tps.report;

import com.ecommerce.tps.api.ApiDtos.ProductResponse;
import com.ecommerce.tps.api.ApiDtos.TopCustomerResponse;
import com.ecommerce.tps.api.ApiDtos.TopProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reportes", description = "Reportes de catálogo, ventas y clientes")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
	private final ReportService reportService;
	public ReportController(ReportService reportService) { this.reportService = reportService; }

	@GetMapping("/products/active")
	@Operation(summary = "Productos activos", description = "Lista el catálogo activo de forma paginada, ordenado por nombre. Requiere rol ADMIN.")
	public Page<ProductResponse> activeProducts(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return reportService.activeProducts(PageRequest.of(page, size, Sort.by("name").ascending()));
	}

	@GetMapping("/products/top-selling")
	@Operation(summary = "Top 5 productos más vendidos", description = "Devuelve hasta cinco productos ordenados por unidades vendidas en órdenes completadas. Requiere rol ADMIN.")
	public List<TopProductResponse> topSellingProducts() { return reportService.topSellingProducts(); }

	@GetMapping("/customers/frequent")
	@Operation(summary = "Top 5 clientes frecuentes", description = "Devuelve hasta cinco clientes ordenados por cantidad de órdenes completadas. Requiere rol ADMIN.")
	public List<TopCustomerResponse> topFrequentCustomers() { return reportService.topFrequentCustomers(); }
}
