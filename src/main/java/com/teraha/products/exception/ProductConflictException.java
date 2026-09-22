package com.teraha.products.exception;

public class ProductConflictException extends RuntimeException {
	public ProductConflictException(String code){
		super("The product with code " + code + " already exists");
	}
}
