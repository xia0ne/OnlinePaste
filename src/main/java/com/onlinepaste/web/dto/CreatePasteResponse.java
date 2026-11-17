package com.onlinepaste.web.dto;

import com.onlinepaste.domain.Paste;

import java.time.Instant;

public record CreatePasteResponse(
		String id,
		String url,
		String qrUrl,
		Instant createdAt,
		Instant expiresAt,
		boolean permanent) {

	public static CreatePasteResponse from(Paste paste, String url) {
		return new CreatePasteResponse(
				paste.id(),
				url,
				"/api/pastes/" + paste.id() + "/qr.svg",
				paste.createdAt(),
				paste.expiresAt(),
				paste.permanent());
	}
}
