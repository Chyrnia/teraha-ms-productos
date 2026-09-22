package com.teraha.products.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import com.teraha.commons.entities.Product;
import com.teraha.commons.dtos.ProductDTO;

import com.teraha.products.repository.ProductRepository;
import com.teraha.products.mapper.ProductMapper;
import com.teraha.products.configs.PagingConfigs;
import com.teraha.products.exception.ProductNotFoundException;
import com.teraha.products.exception.ProductConflictException;

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

		boolean exists = productRepository.existsByCode(dto.getCode());
		if(exists) throw new ProductConflictException(dto.getCode());

		log.info("Creating product with code {}", dto.getCode());

		Product product = productMapper.toEntity(dto);
		Product saved = productRepository.save(product);
		return productMapper.toDto(saved);
	}

	@Transactional(readOnly=true)
	public ProductDTO findProductById(Long id){
		return productRepository.findByIdAndActiveTrue(id)
			.map(productMapper::toDto)
			.orElseThrow(() -> new ProductNotFoundException(id));
	}

	public Optional<ProductDTO> updateProduct(Long id, ProductDTO dto){
		return productRepository.findById(id)
			.map(product -> {
				productMapper.updateEntity(dto, product);
				return productMapper.toDto(productRepository.save(product));
			});
	}

	public boolean deactivateProduct(Long id){
		Optional<Product> optProduct = productRepository.findById(id);
		if(optProduct.isPresent()){
			log.info("Deactivating product with id {}", id);
			Product p = optProduct.get();
			p.setActive(false);
			productRepository.save(p);
			return true;
		}
		return false;
	}

	@Transactional(readOnly=true)
	public Page<ProductDTO> listProducts(int page){
		Pageable pageReq = PageRequest.of(page, pageConfigs.getPageSize());
		return productRepository.findAllByActiveTrue(pageReq)
			.map(productMapper::toDto);
	}

}
