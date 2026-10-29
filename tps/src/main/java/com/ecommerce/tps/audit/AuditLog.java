package com.ecommerce.tps.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 254)
	private String actorEmail;

	@Column(nullable = false, length = 80)
	private String action;

	@Column(nullable = false, length = 80)
	private String entityType;

	@Column(length = 100)
	private String entityId;

	@Column(length = 2000)
	private String details;

	@Column(nullable = false, updatable = false)
	private Instant occurredAt = Instant.now();

	protected AuditLog() {
	}

	public AuditLog(String actorEmail, String action, String entityType, String entityId, String details) {
		this.actorEmail = actorEmail;
		this.action = action;
		this.entityType = entityType;
		this.entityId = entityId;
		this.details = details;
	}

	public Long getId() { return id; }
	public String getActorEmail() { return actorEmail; }
	public String getAction() { return action; }
	public String getEntityType() { return entityType; }
	public String getEntityId() { return entityId; }
	public String getDetails() { return details; }
	public Instant getOccurredAt() { return occurredAt; }
}
