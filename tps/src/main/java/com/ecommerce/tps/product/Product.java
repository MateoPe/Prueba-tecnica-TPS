package com.ecommerce.tps.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 80)
	private String sku;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false, length = 100)
	private String category;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Product() {
	}

	public Product(String sku, String name, String description, String category, BigDecimal price, boolean active) {
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.category = category;
		this.price = price;
		this.active = active;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() { updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public String getSku() { return sku; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public String getCategory() { return category; }
	public BigDecimal getPrice() { return price; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void update(String sku, String name, String description, String category, BigDecimal price, boolean active) {
		this.sku = sku;
		this.name = name;
		this.description = description;
		this.category = category;
		this.price = price;
		this.active = active;
	}
}
