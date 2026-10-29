package com.ecommerce.tps.order;

import com.ecommerce.tps.user.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_orders")
public class CustomerOrder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private UserAccount customer;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<OrderItem> items = new ArrayList<>();

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(nullable = false, precision = 5, scale = 2)
	private BigDecimal discountPercent;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@Column(nullable = false)
	private boolean randomOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status = OrderStatus.COMPLETED;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected CustomerOrder() {
	}

	public CustomerOrder(UserAccount customer, BigDecimal subtotal, BigDecimal discountPercent,
			BigDecimal total, boolean randomOrder) {
		this.customer = customer;
		this.subtotal = subtotal;
		this.discountPercent = discountPercent;
		this.total = total;
		this.randomOrder = randomOrder;
	}

	@PrePersist
	void onCreate() { createdAt = Instant.now(); }

	public void addItem(OrderItem item) {
		items.add(item);
		item.setOrder(this);
	}

	public Long getId() { return id; }
	public UserAccount getCustomer() { return customer; }
	public List<OrderItem> getItems() { return items; }
	public BigDecimal getSubtotal() { return subtotal; }
	public BigDecimal getDiscountPercent() { return discountPercent; }
	public BigDecimal getTotal() { return total; }
	public boolean isRandomOrder() { return randomOrder; }
	public OrderStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public void cancel() { status = OrderStatus.CANCELLED; }
}
