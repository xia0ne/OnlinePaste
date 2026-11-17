package com.onlinepaste.web.dto;

import com.onlinepaste.domain.Paste;
import com.onlinepaste.domain.PasteWithViews;

import java.time.Instant;

public record PasteResponse(
		String id,
		String content,
		Instant createdAt,
		Instant expiresAt,
		boolean permanent,
		long views) {

	public static PasteResponse from(PasteWithViews result) {
		Paste paste = result.paste();
		return new PasteResponse(
				paste.id(),
				paste.content(),
				paste.createdAt(),
				paste.expiresAt(),
				paste.permanent(),
				result.views());
	}
}
