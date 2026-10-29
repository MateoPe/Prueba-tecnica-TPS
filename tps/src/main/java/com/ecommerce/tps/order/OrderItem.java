package com.ecommerce.tps.order;

import com.ecommerce.tps.product.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false, length = 160)
	private String productName;

	@Column(nullable = false, length = 80)
	private String productSku;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	protected OrderItem() {
	}

	public OrderItem(Product product, int quantity, BigDecimal unitPrice) {
		this.product = product;
		this.productName = product.getName();
		this.productSku = product.getSku();
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

	void setOrder(CustomerOrder order) { this.order = order; }
	public Long getId() { return id; }
	public Product getProduct() { return product; }
	public String getProductName() { return productName; }
	public String getProductSku() { return productSku; }
	public int getQuantity() { return quantity; }
	public BigDecimal getUnitPrice() { return unitPrice; }
	public BigDecimal getLineTotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
