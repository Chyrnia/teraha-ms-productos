package com.teraha.products.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.teraha.commons.entities.Product;

public interface ProductRepository extends BaseRepository<Product> {

	Optional<Product> findByIdAndActiveTrue(Long id);

	Page<Product> findAllByActiveTrue(Pageable page);

	@Query("""
		SELECT p FROM Product p
		where p.active = true
		and (
			lower(p.code) like lower(concat('%', :query, '%'))
			or lower(p.description) like lower(concat('%', :query, '%'))
			)
		""")
	Page<Product> searchActiveProducts(@Param("query") String query, Pageable page);

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);
}
