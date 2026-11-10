package com.teraha.products.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import jakarta.persistence.LockModeType;

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

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT p FROM Product p
			WHERE p.id = :id AND p.active = true
		""")
	Optional<Product> findByIdAndActiveTrueForUpdate(@Param("id") Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			SELECT p FROM Product p
			WHERE p.id IN :ids AND p.active = true
			ORDER BY p.id
		""")
	List<Product> findActiveByIdsForUpdate(@Param("ids") List<Long> ids);

	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Long id);
}
