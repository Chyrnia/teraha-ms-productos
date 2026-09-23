package com.teraha.products.service;

import java.util.Optional;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import com.teraha.commons.entities.Product;
import com.teraha.commons.dtos.ProductDTO;
import com.teraha.commons.dtos.InventoryUpdate;

import com.teraha.products.repository.ProductRepository;
import com.teraha.products.mapper.ProductMapper;
import com.teraha.products.configs.PagingConfigs;
import com.teraha.products.exception.ProductNotFoundException;
import com.teraha.products.exception.ProductConflictException;
import com.teraha.products.exception.ProductBadRequestException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ProductService {

	private final PagingConfigs pageConfigs;
	private final ProductRepository productRepository;
	private final ProductMapper productMapper;

	public ProductDTO createProduct(ProductDTO dto){

		validateProductCreationInfo(dto);

		log.info("Creating product with code {}", dto.getCode());

		Product product = productMapper.toEntity(dto);

		//Set creation defaults
		product.setActive(true); 
		product.setStock(0);
		product.setCost(BigDecimal.ZERO);

		Product saved = productRepository.save(product);
		return productMapper.toDto(saved);
	}

	@Transactional(readOnly=true)
	public ProductDTO findProductById(Long id){
		return productRepository.findByIdAndActiveTrue(id)
			.map(productMapper::toDto)
			.orElseThrow(() -> new ProductNotFoundException(id));
	}

	public ProductDTO updateProduct(Long id, ProductDTO dto){

		validateProductUpdateInfo(id, dto);

		return productRepository.findByIdAndActiveTrue(id)
			.map(product -> {

				//check if the code supplied isn't already used in some other product
				checkCodeConflict(id, dto);
				productMapper.updateEntity(dto, product);
				return productMapper.toDto(productRepository.save(product));
			})
			.orElseThrow(() -> new ProductNotFoundException(id));
	}

	public ProductDTO updateOneFieldOfProduct(Long id, ProductDTO dto){

		if(dto.getActive() != null){

			if(dto.getActive() == false || !hasOnlyActiveAttribute(dto)){
				throw new ProductBadRequestException(
						"Active can only be set to true by itself through PATCH"
						);
			}

			Product product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));

			product.setActive(true);
			return productMapper.toDto(productRepository.save(product));
		}

		if(!hasExactlyOneProductUpdateAttribute(dto)){ 
			throw new ProductBadRequestException("Only one attribute may be patched at a time");
		}

		validateForbiddenUpdateFields(dto);

		return productRepository.findByIdAndActiveTrue(id)
			.map(product -> {

				//check if the code supplied isn't already used in some other product
				checkCodeConflict(id, dto);
				productMapper.updateEntity(dto, product);
				return productMapper.toDto(productRepository.save(product));
			})
			.orElseThrow(() -> new ProductNotFoundException(id));
	}

	public void deactivateProduct(Long id){
		Product product = productRepository.findByIdAndActiveTrue(id)
			.orElseThrow(() -> new ProductNotFoundException(id));

		log.info("Deactivating product with id {}", id);
		product.setActive(false);
		productRepository.save(product);
	}

	@Transactional(readOnly=true)
	public Page<ProductDTO> listProducts(int page){
		Pageable pageReq = PageRequest.of(page, pageConfigs.getPageSize());
		return productRepository.findAllByActiveTrue(pageReq)
			.map(productMapper::toDto);
	}

	public ProductDTO updateProductInventory(Long id, InventoryUpdate dto){

		Product product = productRepository.findByIdAndActiveTrue(id)
			.orElseThrow(() -> new ProductNotFoundException(id));

		validateInventoryUpdate(dto, product);


		//If we are here, then the data is valid and it's only a matter of applying changes
		if(dto.getUnitCost() != null){
			product.setCost(BigDecimal.valueOf(dto.getUnitCost()));
		}

		int stock = product.getStock();

		product.setStock(stock + dto.getStockDelta());

		log.info("Updating stock for product with id {}: A stock delta of {} [{} -> {}]", 
				id, dto.getStockDelta(), stock, product.getStock());
		 
		return productMapper.toDto(product);
	}

	private void validateInventoryUpdate(InventoryUpdate dto, Product product){

		if(dto.getStockDelta() == null || dto.getStockDelta() == 0){
			throw new ProductBadRequestException("Stock Delta must be provided and can't be zero"); 
		}


		if((dto.getStockDelta() < 0) && dto.getUnitCost() != null){
			throw new ProductBadRequestException("A new sale can't provide a unit cost");
		}

		if(dto.getStockDelta() > 0){
			if(dto.getUnitCost() == null || dto.getUnitCost() <= 0){
				throw new ProductBadRequestException("Unit cost must be provided and be bigger than zero"); 
			}
		}

		if(product.getStock() + dto.getStockDelta() < 0){
			throw new ProductBadRequestException("Can't sell more products than we currently have");
		}
	}

	private boolean hasExactlyOneProductUpdateAttribute(ProductDTO dto){
		int count = 0;

		if (dto.getCode() != null)  count++;
		if (dto.getDescription() != null)  count++;
		if (dto.getPrice() != null)  count++;
		if (dto.getMargin() != null)  count++;
		if (!dto.getCategories().isEmpty()) count++;
		if (!dto.getSuppliers().isEmpty()) count++;

		return count == 1;
	}

	private boolean hasOnlyActiveAttribute(ProductDTO dto){
		return dto.getCode() == null
			&& dto.getDescription() == null
			&& dto.getStock() == null
			&& dto.getPrice() == null
			&& dto.getCost() == null
			&& dto.getMargin() == null
			&& dto.getCategories().isEmpty() 
			&& dto.getSuppliers().isEmpty();
	}

	private boolean hasAtLeastOneProductUpdateAttribute(ProductDTO dto){
		return dto.getCode() != null
			|| dto.getDescription() != null
			|| dto.getPrice() != null
			|| dto.getMargin() != null
			|| !dto.getCategories().isEmpty() 
			|| !dto.getSuppliers().isEmpty();
	}

	private void validateProductUpdateInfo(Long id, ProductDTO dto){
		//check we have enough info
		boolean enoughInfo = hasAtLeastOneProductUpdateAttribute(dto);
		if(!enoughInfo) {
			throw new ProductBadRequestException(
					"Should change at least one of Code, Description, Price, Margin, Categories or Suppliers"
					);
		}
		//check for forbidden changes
		validateForbiddenUpdateFields(dto);
	}

	private void validateProductCreationInfo(ProductDTO dto){

		//check we have enough info
		validateRequiredCreationFields(dto);

		//check they aren't supplying forbidden fields
		validateForbiddenCreationFields(dto);

		//check if the product code already exists
		boolean exists = productRepository.existsByCode(dto.getCode());
		if(exists) throw new ProductConflictException(dto.getCode());

	}

	private void validateRequiredCreationFields(ProductDTO dto){
		if ( dto.getCode() == null || dto.getDescription() == null || 
				dto.getPrice() == null || dto.getMargin() == null )
		{

			throw new ProductBadRequestException( 
					"Code, Description, Price and Margin MUST be provided"
					);

		}
	}

	private void validateForbiddenUpdateFields(ProductDTO dto){
		if(dto.getStock() != null || dto.getCost() != null || dto.getActive() != null){
			throw new ProductBadRequestException(
					"Stock, Cost or Active status can't be changed in this way"
					);
		}
	}

	private void validateForbiddenCreationFields(ProductDTO dto){
		if(dto.getStock() != null || dto.getCost() != null || dto.getActive() != null){
			throw new ProductBadRequestException(
					"Stock, Cost and Active status can't be supplied on creation"
					);
		}
	}

	private void checkCodeConflict(Long id, ProductDTO dto){
		if(dto.getCode() == null){
			return;
		}

		boolean exists = productRepository.existsByCodeAndIdNot(dto.getCode(), id);

		if(exists){
			throw new ProductConflictException(dto.getCode());
		}
	}

}
