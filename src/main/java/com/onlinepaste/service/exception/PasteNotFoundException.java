package com.onlinepaste.service.exception;

/**
 * The requested paste does not exist or has already expired.
 */
public class PasteNotFoundException extends RuntimeException {

	public PasteNotFoundException(String id) {
		super("Paste not found: " + id);
	}
}
