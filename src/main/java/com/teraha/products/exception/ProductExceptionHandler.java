package com.teraha.products.exception;

import java.time.OffsetDateTime;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.teraha.commons.dtos.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class ProductExceptionHandler {

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleProductNotFound(ProductNotFoundException exception){
		ErrorResponse error = new ErrorResponse()
			.status(HttpStatus.NOT_FOUND.value())
			.message(exception.getMessage())
			.timestamp(OffsetDateTime.now());
		return new ResponseEntity<>(error, HttpStatus.NOT_FOUND); 
	}

	@ExceptionHandler(ProductConflictException.class)
	public ResponseEntity<ErrorResponse> handleProductConflict(ProductConflictException exception){
		ErrorResponse error = new ErrorResponse()
			.status(HttpStatus.CONFLICT.value())
			.message(exception.getMessage())
			.timestamp(OffsetDateTime.now());
		return new ResponseEntity<>(error, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(ProductBadRequestException.class)
	public ResponseEntity<ErrorResponse> handleBadRequest(ProductBadRequestException exception){
		ErrorResponse error = new ErrorResponse()
			.status(HttpStatus.BAD_REQUEST.value())
			.message(exception.getMessage())
			.timestamp(OffsetDateTime.now());
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericError(Exception exception){

		log.error("Unexpected error while processing product request", exception);

		ErrorResponse error = new ErrorResponse()
			.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
			.message(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
			.timestamp(OffsetDateTime.now());
		return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
