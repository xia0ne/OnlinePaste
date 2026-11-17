package com.onlinepaste.service;

import com.onlinepaste.domain.Paste;
import com.onlinepaste.domain.PasteWithViews;
import com.onlinepaste.web.dto.CreatePasteRequest;

public interface PasteService {

	/**
	 * Creates a new paste with the requested lifetime.
	 *
	 * @throws com.onlinepaste.service.exception.PasteValidationException invalid payload
	 * @throws com.onlinepaste.service.exception.PasteConflictException  custom id already in use
	 */
	Paste create(CreatePasteRequest request);

	/**
	 * Loads a paste and optionally counts the view.
	 *
	 * @throws com.onlinepaste.service.exception.PasteNotFoundException unknown or expired id
	 */
	PasteWithViews get(String id, boolean countView);

	/**
	 * Absolute share URL, based on the configured public base URL.
	 */
	String shareUrl(String id);

	/**
	 * QR code for the share URL, rendered as an SVG document.
	 */
	String qrSvg(String id);
}
