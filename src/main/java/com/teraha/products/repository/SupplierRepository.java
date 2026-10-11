package com.teraha.products.repository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teraha.commons.entities.Supplier;

public interface SupplierRepository extends PersonRepository<Supplier> {

	Page<Supplier> findAllByActiveTrue(Pageable page);

	Optional<Supplier> findByIdAndActiveTrue(Long id);
}
