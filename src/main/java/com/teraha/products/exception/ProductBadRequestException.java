package com.teraha.products.exception;

public class ProductBadRequestException extends RuntimeException {
	public ProductBadRequestException(String message){
		super(message);
	}
}
