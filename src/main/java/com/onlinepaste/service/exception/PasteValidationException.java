package com.onlinepaste.service.exception;

/**
 * Request payload or path parameter did not pass validation.
 */
public class PasteValidationException extends RuntimeException {

	public PasteValidationException(String message) {
		super(message);
	}
}
