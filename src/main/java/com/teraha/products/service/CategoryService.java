package com.teraha.products.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CachePut;

import com.teraha.commons.entities.Category;
import com.teraha.commons.dtos.CategoryDTO;

import com.teraha.products.repository.CategoryRepository;
import com.teraha.products.mapper.ProductMapper;
import com.teraha.products.exception.CategoryConflictException;
import com.teraha.products.exception.ProductBadRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @CachePut(cacheNames = "products:categories", key = "#result.id")
    public CategoryDTO createCategory(CategoryDTO dto) {

        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            throw new ProductBadRequestException("Category name cannot be empty");
        }

        String name = dto.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CategoryConflictException(name);
        }

        log.info("Creating category with name: {}", name);

        Category category = new Category(name);
        Category saved = categoryRepository.save(category);

        return productMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> searchCategories(String query) {

        List<Category> categories;

        if (query == null || query.isBlank()) {
            categories = categoryRepository.findAllByOrderByNameAsc();
        } else {
            categories = categoryRepository
                .findByNameContainingIgnoreCaseOrderByNameAsc(query.trim());
        }

        return categories.stream()
            .map(productMapper::toDto)
            .toList();
    }
}
