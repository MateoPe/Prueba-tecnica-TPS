package com.ecommerce.tps.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
	@Query("""
		select p from Product p
		where (:active is null or p.active = :active)
		  and (:term = '' or lower(p.name) like lower(concat('%', :term, '%'))
		       or lower(p.sku) like lower(concat('%', :term, '%'))
		       or (p.description is not null and lower(p.description) like lower(concat('%', :term, '%'))))
		  and (:category = '' or lower(p.category) = lower(:category))
		""")
	Page<Product> search(@Param("active") Boolean active, @Param("term") String term,
			@Param("category") String category, Pageable pageable);
}
