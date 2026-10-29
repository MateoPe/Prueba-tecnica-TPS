package com.ecommerce.tps.inventory;

import com.ecommerce.tps.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "inventory")
public class Inventory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false, unique = true)
	private Product product;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private Instant updatedAt = Instant.now();

	@Version
	private long version;

	protected Inventory() {
	}

	public Inventory(Product product, int quantity) {
		this.product = product;
		this.quantity = quantity;
	}

	public Long getId() { return id; }
	public Product getProduct() { return product; }
	public int getQuantity() { return quantity; }
	public Instant getUpdatedAt() { return updatedAt; }
	public void add(int amount) {
		quantity += amount;
		updatedAt = Instant.now();
	}
	public void subtract(int amount) {
		quantity -= amount;
		updatedAt = Instant.now();
	}
}
