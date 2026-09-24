package com.teraha.products.controller;

import com.teraha.commons.dtos.ProductDTO;
import com.teraha.commons.dtos.InventoryUpdate;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Tag(name = "Products", description="Product management endpoints")
@Slf4j
@RestController
@RequestMapping(path="/products", produces="application/json")
@CrossOrigin
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@Operation( summary="Get a list of products", description="Returns a page of active products, optionally filtered by a search string")
	@ApiResponses({
		@ApiResponse(responseCode="200", description="Products page with matching products") 
		})
	@GetMapping
	public Page<ProductDTO> getProducts(@RequestParam(name="page", defaultValue="0") int page, 
			@RequestParam(name="search", required=false) String search){

			log.info("Received a GET request at /products: page={} search={}", page, search);

			if(search != null && !search.isBlank()){
				return productService.searchProducts(search, page); 
			}
			return productService.listProducts(page);
	}

	@Operation(summary="Get a product", description="Returns an active product by its id")
	@ApiResponses({
		@ApiResponse(responseCode="200", description="Product found"),
		@ApiResponse(responseCode="404", description="Product not found")
		})
	@GetMapping("/{id}")
	public ResponseEntity<ProductDTO> findProductById(@PathVariable("id") Long id){

		log.info("Received a GET request at /products/{}", id);

		ProductDTO product = productService.findProductById(id);
		return new ResponseEntity<>(product, HttpStatus.OK);
	}

	@Operation(summary="Create a product", description="Creates an active product with default values for cost and stock")
	@ApiResponses({
		@ApiResponse(responseCode="201", description="Product created"),
		@ApiResponse(responseCode="400", description="Malformed request"),
		@ApiResponse(responseCode="409", description="Product code already in use")
		})
	@PostMapping(consumes = "application/json")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductDTO postProduct(@RequestBody ProductDTO dto){

		log.info("Received a POST request at /products");

		return productService.createProduct(dto);
	}

	@Operation(summary="Updates a product", description="Updates one or more attributes of an active product")
	@ApiResponses({
		@ApiResponse(responseCode="200", description="Product updated"),
		@ApiResponse(responseCode="400", description="Malformed request"),
		@ApiResponse(responseCode="404", description="Product id not found"),
		@ApiResponse(responseCode="409", description="Product code already in use")
		})
	@PutMapping(path="/{id}", consumes="application/json")
	public ResponseEntity<ProductDTO> putProduct(@PathVariable("id") Long id, @RequestBody ProductDTO dto){

		log.info("Received a PUT request at /products/{}", id);

		ProductDTO product = productService.updateProduct(id, dto);
		return ResponseEntity.ok(product);
	}

	@Operation(summary="Update one field of a Product", description="Updates one field of an active Product, or reactivates an inactive product")
	@ApiResponses({
		@ApiResponse(responseCode="200", description="Product updated or reactivated"),
		@ApiResponse(responseCode="400", description="Malformed request"),
		@ApiResponse(responseCode="404", description="Product id not found"),
		@ApiResponse(responseCode="409", description="Product code already in use")
		})
	@PatchMapping(path="/{id}", consumes="application/json")
	public ResponseEntity<ProductDTO> patchProduct(@PathVariable("id") Long id, @RequestBody ProductDTO dto){

		log.info("Received a PATCH request at /products/{}", id);

		ProductDTO product = productService.updateOneFieldOfProduct(id, dto);
		return ResponseEntity.ok(product);
	}

	@Operation(summary="Deactivate a product", description="Select a product by id and disable it")
	@ApiResponses({
		@ApiResponse(responseCode="204", description="Product deactivated"),
		@ApiResponse(responseCode="404", description="Product id not found")
		})
	@DeleteMapping(path="/{id}")
	public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id){

		log.info("Received a DELETE request at /products/{}", id);

		productService.deactivateProduct(id);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary="Update a product's inventory", description="Updates a product's stock, and for Purchases, its current cost")
	@ApiResponses({
		@ApiResponse(responseCode="200", description="Product inventory/cost updated"),
		@ApiResponse(responseCode="400", description="Malformed request"),
		@ApiResponse(responseCode="404", description="Product id not found")
		})
	@PatchMapping(path="/{id}/inventory", consumes="application/json")
	public ResponseEntity<ProductDTO> patchProductInventory(@PathVariable("id") Long id, @RequestBody InventoryUpdate dto){

		log.info("Received a PATCH request at /products/{}/inventory", id);

		ProductDTO product = productService.updateProductInventory(id, dto);
		return ResponseEntity.ok(product);
	}
}
