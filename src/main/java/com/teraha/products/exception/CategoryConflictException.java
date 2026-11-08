package com.teraha.products.exception;

public class CategoryConflictException extends RuntimeException {
	public CategoryConflictException(String name){
		super("A category named '" + name + "' already exists");
	}
}
