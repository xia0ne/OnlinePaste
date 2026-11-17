package com.onlinepaste.service.exception;

/**
 * The requested id is already taken by another paste.
 */
public class PasteConflictException extends RuntimeException {

	public PasteConflictException(String message) {
		super(message);
	}
}
