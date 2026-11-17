package com.onlinepaste.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Payload for {@code POST /api/pastes}.
 */
public record CreatePasteRequest(
		@NotBlank(message = "content must not be blank")
		@Size(max = 200_000, message = "content is too large")
		String content,

		@Pattern(regexp = "^(1h|1d|7d|never)?$", message = "expiration must be one of 1h, 1d, 7d, never")
		String expiration,

		@Pattern(regexp = "^[A-Za-z0-9_-]{4,64}$", message = "id must be 4-64 characters of letters, digits, - or _")
		String id) {
}
