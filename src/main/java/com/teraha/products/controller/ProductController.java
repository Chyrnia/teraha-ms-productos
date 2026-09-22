package com.teraha.products.controller;

import java.util.Optional;

import com.teraha.commons.dtos.ProductDTO;
import com.teraha.products.service.ProductService;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path="/products", produces="application/json")
@CrossOrigin
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@GetMapping("/{id}")
	public ResponseEntity<ProductDTO> findProductById(@PathVariable("id") Long id){
		ProductDTO product = productService.findProductById(id);
		return new ResponseEntity<>(product, HttpStatus.OK);
	}

	@PostMapping(consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductDTO postProduct(@RequestBody ProductDTO dto){
		 return productService.createProduct(dto);
	}

	@GetMapping
	public Page<ProductDTO> getProducts(@RequestParam(name="page", defaultValue="0") int page){
		return productService.listProducts(page);
	}

	@PutMapping(path="/{id}", consumes="application/json")
	public ResponseEntity<ProductDTO> putProduct(
			@PathVariable("id") Long id,
			@RequestBody ProductDTO dto)
	{
		Optional<ProductDTO> product = productService.updateProduct(id, dto);

		if(product.isPresent()){
			return ResponseEntity.ok(product.get());
		}

		return ResponseEntity.notFound().build();
	}

	@DeleteMapping(path="/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id){
		boolean deleted = productService.deactivateProduct(id);

		if (deleted) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.notFound().build();
	}

}
