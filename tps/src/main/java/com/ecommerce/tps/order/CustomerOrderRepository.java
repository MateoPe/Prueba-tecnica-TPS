package com.ecommerce.tps.order;

import com.ecommerce.tps.user.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from CustomerOrder o where o.id = :id")
	java.util.Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);

	Page<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

	@Query("select count(o) from CustomerOrder o where o.customer.id = :customerId and o.status = :status")
	long countCompletedByCustomer(@Param("customerId") Long customerId, @Param("status") OrderStatus status);

	@Query("""
		select i.product.id, i.productName, i.productSku, sum(i.quantity)
		from OrderItem i join i.order o
		where o.status = 'COMPLETED'
		group by i.product.id, i.productName, i.productSku
		order by sum(i.quantity) desc
		""")
	List<Object[]> topSellingProducts(Pageable pageable);

	@Query("""
		select o.customer.id, o.customer.fullName, o.customer.email, count(o)
		from CustomerOrder o
		where o.status = 'COMPLETED'
		group by o.customer.id, o.customer.fullName, o.customer.email
		order by count(o) desc
		""")
	List<Object[]> topFrequentCustomers(Pageable pageable);

	List<CustomerOrder> findByCustomerAndStatusAndCreatedAtBetween(UserAccount customer, OrderStatus status,
			Instant from, Instant to);
}
