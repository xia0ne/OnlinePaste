package com.onlinepaste.web;

import com.onlinepaste.service.exception.PasteConflictException;
import com.onlinepaste.service.exception.PasteNotFoundException;
import com.onlinepaste.service.exception.PasteValidationException;
import com.onlinepaste.web.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.onlinepaste.controller.PasteApiController;

@RestControllerAdvice(assignableTypes = PasteApiController.class)
public class PasteApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(PasteApiExceptionHandler.class);

	@ExceptionHandler(PasteNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(PasteNotFoundException e) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError("not_found", "This paste does not exist or has expired."));
	}

	@ExceptionHandler(PasteConflictException.class)
	public ResponseEntity<ApiError> handleConflict(PasteConflictException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiError("conflict", e.getMessage()));
	}

	@ExceptionHandler(PasteValidationException.class)
	public ResponseEntity<ApiError> handleValidation(PasteValidationException e) {
		return ResponseEntity.badRequest()
				.body(new ApiError("validation_error", e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleBeanValidation(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(FieldError::getDefaultMessage)
				.orElse("invalid request");
		return ResponseEntity.badRequest()
				.body(new ApiError("validation_error", message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest()
				.body(new ApiError("malformed_json", "Request body must be valid JSON."));
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiError> handleStorage(DataAccessException e) {
		log.error("Storage error while handling API request", e);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
				.body(new ApiError("storage_unavailable", "Storage is temporarily unavailable."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception e) {
		log.error("Unexpected error while handling API request", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiError("internal_error", "Unexpected server error."));
	}
}
