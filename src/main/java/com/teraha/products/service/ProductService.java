
package com.teraha.products.service;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.CacheEvict;

import com.teraha.commons.entities.Product;
import com.teraha.commons.entities.Category;
import com.teraha.commons.entities.Supplier;
import com.teraha.commons.dtos.ProductDTO;
import com.teraha.commons.dtos.InventoryUpdate;
import com.teraha.commons.dtos.InventoryUpdateLine;

import com.teraha.products.repository.ProductRepository;
import com.teraha.products.repository.CategoryRepository;
import com.teraha.products.repository.SupplierRepository;
import com.teraha.products.mapper.ProductMapper;
import com.teraha.products.configs.PagingConfigs;
import com.teraha.products.exception.ProductNotFoundException;
import com.teraha.products.exception.ProductConflictException;
import com.teraha.products.exception.ProductBadRequestException;
import com.teraha.products.exception.CategoryNotFoundException;
import com.teraha.products.exception.SupplierNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ProductService {

    private final PagingConfigs pageConfigs;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductMapper productMapper;

    @CachePut(cacheNames = "products:products", key = "#result.id")
    public ProductDTO createProduct(ProductDTO dto) {

        validateProductCreationInfo(dto);

        log.info("Creating product with code {}", dto.getCode());

        Product product = productMapper.toEntity(dto);

        addSuppliers(product, dto.getSupplierIds());
        addCategories(product, dto.getCategoryIds());

        // Set creation defaults
        product.setActive(true);
        product.setStock(0);
        product.setCost(BigDecimal.ZERO);

        Product saved = productRepository.save(product);
        return productMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "products:products", key = "#id")
    public ProductDTO findProductById(Long id) {

        return productRepository.findByIdAndActiveTrue(id)
            .map(productMapper::toDto)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CachePut(cacheNames = "products:products", key = "#id")
    public ProductDTO updateProduct(Long id, ProductDTO dto) {

        validateProductUpdateInfo(id, dto);

        return productRepository.findByIdAndActiveTrue(id)
            .map(product -> {

                checkCodeConflict(id, dto);
                productMapper.updateEntity(dto, product);

                if (dto.getSupplierIds() != null) {
                    replaceSuppliers(product, dto.getSupplierIds());
                }

                if (dto.getCategoryIds() != null) {
                    replaceCategories(product, dto.getCategoryIds());
                }

                return productMapper.toDto(productRepository.save(product));
            })
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CachePut(cacheNames = "products:products", key = "#id")
    public ProductDTO updateOneFieldOfProduct(Long id, ProductDTO dto) {

        if (dto.getActive() != null) {

            if (dto.getActive() == false || !hasOnlyActiveAttribute(dto)) {
                throw new ProductBadRequestException(
                    "Active can only be set to true by itself through PATCH"
                );
            }

            Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

            product.setActive(true);
            return productMapper.toDto(productRepository.save(product));
        }

        if (!hasExactlyOneProductUpdateAttribute(dto)) {
            throw new ProductBadRequestException(
                "Only one attribute may be patched at a time"
            );
        }

        validateForbiddenUpdateFields(dto);

        return productRepository.findByIdAndActiveTrue(id)
            .map(product -> {

                checkCodeConflict(id, dto);
                productMapper.updateEntity(dto, product);

                if (dto.getSupplierIds() != null) {
                    replaceSuppliers(product, dto.getSupplierIds());
                }

                if (dto.getCategoryIds() != null) {
                    replaceCategories(product, dto.getCategoryIds());
                }

                return productMapper.toDto(productRepository.save(product));
            })
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @CacheEvict(cacheNames = "products:products", key = "#id")
    public void deactivateProduct(Long id) {

        Product product = productRepository.findByIdAndActiveTrue(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        log.info("Deactivating product with id {}", id);

        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> listProducts(int page) {

        Pageable pageReq = PageRequest.of(
            page,
            pageConfigs.getPageSize()
        );

        return productRepository.findAllByActiveTrue(pageReq)
            .map(productMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String query, int page) {

        Pageable pageReq = PageRequest.of(
            page,
            pageConfigs.getPageSize()
        );

        return productRepository.searchActiveProducts(query, pageReq)
            .map(productMapper::toDto);
    }

    /*
     * Single-product inventory update.
     *
     * Kept for compatibility with the existing PATCH endpoint.
     * It delegates to the batch implementation to avoid maintaining
     * two different inventory-update mechanisms.
     */
    public ProductDTO updateProductInventory(Long id, InventoryUpdate dto) {

        if (dto == null || dto.getDetails() == null
                || dto.getDetails().size() != 1) {

            throw new ProductBadRequestException(
                "Exactly one inventory update must be provided"
            );
        }

        InventoryUpdateLine line = dto.getDetails().get(0);

        if (line.getProductId() == null || !line.getProductId().equals(id)) {
            throw new ProductBadRequestException(
                "The product ID in the request must match the URL"
            );
        }

        updateProductInventories(dto);

        return findProductById(id);
    }

    /*
     * Batch inventory update.
     *
     * All product rows are locked, all movements are validated,
     * and only then are the changes applied.
     */
    @CacheEvict(cacheNames = "products:products", allEntries = true)
    public void updateProductInventories(InventoryUpdate dto) {

        if (dto == null || dto.getDetails() == null
                || dto.getDetails().isEmpty()) {

            throw new ProductBadRequestException(
                "At least one inventory update must be provided"
            );
        }

        // Validate request structure and reject duplicate product IDs.
        Set<Long> productIds = new HashSet<>();

        for (InventoryUpdateLine line : dto.getDetails()) {

            if (line == null || line.getProductId() == null) {
                throw new ProductBadRequestException(
                    "Every inventory update must specify a product ID"
                );
            }

            if (!productIds.add(line.getProductId())) {
                throw new ProductBadRequestException(
                    "A product can only appear once in an inventory update: "
                    + line.getProductId()
                );
            }

            if (line.getStockDelta() == null || line.getStockDelta() == 0) {
                throw new ProductBadRequestException(
                    "Stock delta must be provided and cannot be zero"
                );
            }

            if (line.getUnitCost() != null && line.getUnitCost() <= 0) {
                throw new ProductBadRequestException(
                    "Unit cost must be greater than zero"
                );
            }

            if (line.getStockDelta() < 0 && line.getUnitCost() != null) {
                throw new ProductBadRequestException(
                    "A stock reduction cannot provide a unit cost"
                );
            }
        }

        // Lock all active products in a consistent order.
        List<Product> products =
            productRepository.findActiveByIdsForUpdate(
                new ArrayList<>(productIds)
            );

        if (products.size() != productIds.size()) {

            Set<Long> foundIds = new HashSet<>();
            products.forEach(product -> foundIds.add(product.getId()));

            productIds.removeAll(foundIds);

            throw new ProductBadRequestException(
                "Products not found or inactive: " + productIds
            );
        }

        // Index the locked entities by ID.
        Map<Long, Product> productsById = new HashMap<>();

        products.forEach(product ->
            productsById.put(product.getId(), product)
        );

        // Validate every resulting stock quantity before modifying anything.
        for (InventoryUpdateLine line : dto.getDetails()) {

            Product product = productsById.get(line.getProductId());

			if (product.getStock() + line.getStockDelta() < 0) {
				throw new ProductBadRequestException(
						String.format(
							"Insufficient stock for product '%s' (code: %s). "
							+ "Requested: %d, available: %d.",
							product.getDescription(),
							product.getCode(),
							Math.negateExact(line.getStockDelta()),
							product.getStock()
							)
						);
			}
		}

        // All movements are valid. Apply the changes.
        for (InventoryUpdateLine line : dto.getDetails()) {

            Product product = productsById.get(line.getProductId());

            int previousStock = product.getStock();
            int newStock = previousStock + line.getStockDelta();

            product.setStock(newStock);

            // Only update cost when the caller explicitly provides one.
            if (line.getUnitCost() != null) {
                // If unitCost is BigDecimal, use line.getUnitCost() directly.
                product.setCost(BigDecimal.valueOf(line.getUnitCost()));
            }

            log.info(
                "Updating stock for product {}: {} [{} -> {}]",
                product.getId(),
                line.getStockDelta(),
                previousStock,
                newStock
            );
        }

        productRepository.saveAll(products);
    }

    private void validateProductCreationInfo(ProductDTO dto) {

        validateRequiredCreationFields(dto);
        validateForbiddenCreationFields(dto);

        boolean exists = productRepository.existsByCode(dto.getCode());

        if (exists) {
            throw new ProductConflictException(dto.getCode());
        }
    }

    private void validateRequiredCreationFields(ProductDTO dto) {

        if (dto.getCode() == null
                || dto.getDescription() == null
                || dto.getPrice() == null
                || dto.getMargin() == null) {

            throw new ProductBadRequestException(
                "Code, Description, Price and Margin MUST be provided"
            );
        }
    }

    private void validateForbiddenCreationFields(ProductDTO dto) {

        if (dto.getStock() != null
                || dto.getCost() != null
                || dto.getActive() != null) {

            throw new ProductBadRequestException(
                "Stock, Cost and Active status can't be supplied on creation"
            );
        }
    }

    private void validateProductUpdateInfo(Long id, ProductDTO dto) {

        boolean enoughInfo = hasAtLeastOneProductUpdateAttribute(dto);

        if (!enoughInfo) {
            throw new ProductBadRequestException(
                "Should change at least one of Code, Description, Price, "
                + "Margin, Categories or Suppliers"
            );
        }

        validateForbiddenUpdateFields(dto);
    }

    private void validateForbiddenUpdateFields(ProductDTO dto) {

        if (dto.getStock() != null
                || dto.getCost() != null
                || dto.getActive() != null) {

            throw new ProductBadRequestException(
                "Stock, Cost or Active status can't be changed in this way"
            );
        }
    }

    private boolean hasExactlyOneProductUpdateAttribute(ProductDTO dto) {

        int count = 0;

        if (dto.getCode() != null) count++;
        if (dto.getDescription() != null) count++;
        if (dto.getPrice() != null) count++;
        if (dto.getMargin() != null) count++;
        if (dto.getCategoryIds() != null) count++;
        if (dto.getSupplierIds() != null) count++;

        return count == 1;
    }

    private boolean hasOnlyActiveAttribute(ProductDTO dto) {

        return dto.getCode() == null
            && dto.getDescription() == null
            && dto.getStock() == null
            && dto.getPrice() == null
            && dto.getCost() == null
            && dto.getMargin() == null
            && dto.getCategoryIds() == null
            && dto.getSupplierIds() == null;
    }

    private boolean hasAtLeastOneProductUpdateAttribute(ProductDTO dto) {

        return dto.getCode() != null
            || dto.getDescription() != null
            || dto.getPrice() != null
            || dto.getMargin() != null
            || dto.getCategoryIds() != null
            || dto.getSupplierIds() != null;
    }

    private void checkCodeConflict(Long id, ProductDTO dto) {

        if (dto.getCode() == null) {
            return;
        }

        boolean exists =
            productRepository.existsByCodeAndIdNot(dto.getCode(), id);

        if (exists) {
            throw new ProductConflictException(dto.getCode());
        }
    }

    private void addSuppliers(Product product, List<Long> supplierIds) {

        if (supplierIds != null && !supplierIds.isEmpty()) {

            supplierIds.forEach(supplierId -> {

                Supplier supplier = supplierRepository
                    .findByIdAndActiveTrue(supplierId)
                    .orElseThrow(() ->
                        new SupplierNotFoundException(supplierId)
                    );

                product.getSuppliers().add(supplier);
            });
        }
    }

    private void addCategories(Product product, List<Long> categoryIds) {

        if (categoryIds != null && !categoryIds.isEmpty()) {

            categoryIds.forEach(categoryId -> {

                Category category = categoryRepository
                    .findById(categoryId)
                    .orElseThrow(() ->
                        new CategoryNotFoundException(categoryId)
                    );

                product.getCategories().add(category);
            });
        }
    }

    private void replaceSuppliers(Product product, List<Long> supplierIds) {

        product.getSuppliers().clear();
        addSuppliers(product, supplierIds);
    }

    private void replaceCategories(Product product, List<Long> categoryIds) {

        product.getCategories().clear();
        addCategories(product, categoryIds);
    }
}

