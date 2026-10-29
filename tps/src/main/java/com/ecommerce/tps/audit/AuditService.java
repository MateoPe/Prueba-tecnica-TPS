package com.ecommerce.tps.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
	private final AuditLogRepository repository;

	public AuditService(AuditLogRepository repository) {
		this.repository = repository;
	}

	@Transactional
	public void record(String action, String entityType, Object entityId, String details) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String actor = authentication == null || !authentication.isAuthenticated()
				|| authentication.getName().equals("anonymousUser") ? null : authentication.getName();
		recordAs(actor, action, entityType, entityId, details);
	}

	@Transactional
	public void recordAs(String actor, String action, String entityType, Object entityId, String details) {
		repository.save(new AuditLog(actor, action, entityType,
				entityId == null ? null : entityId.toString(), details));
	}

	@Transactional(readOnly = true)
	public Page<AuditLog> list(Pageable pageable) {
		return repository.findAllByOrderByOccurredAtDesc(pageable);
	}
}
