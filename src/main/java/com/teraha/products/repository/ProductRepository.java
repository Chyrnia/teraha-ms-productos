package com.teraha.products.repository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import com.teraha.commons.entities.Product;

public interface ProductRepository extends BaseRepository<Product> {
	Optional<Product> findByIdAndActiveTrue(Long id);
	Page<Product> findAllByActiveTrue(Pageable page);
	boolean existsByCode(String code);
	boolean existsByCodeAndIdNot(String code, Long id);
}
