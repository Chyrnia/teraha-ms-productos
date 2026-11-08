package com.teraha.products.repository;

import java.util.List;

import com.teraha.commons.entities.Category;

public interface CategoryRepository extends BaseRepository<Category> {

	boolean existsByNameIgnoreCase(String name);

	List<Category> findByNameContainingIgnoreCaseOrderByNameAsc(String query);

	List<Category> findAllByOrderByNameAsc();

}
