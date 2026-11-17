package com.onlinepaste.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * A single clipboard entry. Serialized as JSON into Redis.
 */
public record Paste(String id, String content, Instant createdAt, Instant expiresAt) {

	public boolean permanent() {
		return expiresAt == null;
	}

	/**
	 * @return remaining lifetime, or {@code null} for a permanent paste
	 */
	public Duration remainingTtl(Instant now) {
		if (expiresAt == null) {
			return null;
		}
		Duration remaining = Duration.between(now, expiresAt);
		return remaining.isNegative() ? Duration.ZERO : remaining;
	}
}
