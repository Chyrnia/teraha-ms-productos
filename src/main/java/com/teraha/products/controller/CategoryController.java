package com.teraha.products.controller;

import com.teraha.commons.dtos.CategoryDTO;
import com.teraha.products.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/categories", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Search categories",
            description = "Returns all categories or filters them by name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'SYSADMIN')")
    public List<CategoryDTO> searchCategories(
            @Parameter(description = "Optional category name filter")
            @RequestParam(required = false) String query) {

        log.debug("Searching categories with query: {}", query);
        return categoryService.searchCategories(query);
    }

    @Operation(summary = "Create category",
            description = "Creates a new product category.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid category data"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "409", description = "Category already exists")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'SYSADMIN')")
    public CategoryDTO createCategory(@RequestBody CategoryDTO dto) {

        log.info("Creating category: {}", dto.getName());
        return categoryService.createCategory(dto);
    }
}
