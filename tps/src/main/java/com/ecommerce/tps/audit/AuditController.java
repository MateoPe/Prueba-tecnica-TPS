package com.ecommerce.tps.audit;

import com.ecommerce.tps.api.ApiDtos.AuditLogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Auditoría", description = "Consulta de eventos de auditoría del sistema")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {
	private final AuditService auditService;
	public AuditController(AuditService auditService) { this.auditService = auditService; }

	@GetMapping
	@Operation(summary = "Consultar auditoría", description = "Lista eventos recientes con paginación. Requiere rol ADMIN.")
	public Page<AuditLogResponse> list(@ParameterObject @PageableDefault(size = 50) Pageable pageable) {
		return auditService.list(pageable).map(AuditLogResponse::from);
	}
}
