package com.onlinepaste.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Supported lifetimes for a paste.
 */
public enum Expiration {
	ONE_HOUR("1h", Duration.ofHours(1)),
	ONE_DAY("1d", Duration.ofDays(1)),
	SEVEN_DAYS("7d", Duration.ofDays(7)),
	NEVER("never", null);

	private final String code;
	private final Duration duration;

	Expiration(String code, Duration duration) {
		this.code = code;
		this.duration = duration;
	}

	public String getCode() {
		return code;
	}

	/**
	 * @return time to live, or {@code null} when the paste never expires
	 */
	public Duration getDuration() {
		return duration;
	}

	public Instant expiresAt(Instant createdAt) {
		return duration == null ? null : createdAt.plus(duration);
	}

	public static Optional<Expiration> fromCode(String code) {
		if (code == null || code.isBlank()) {
			return Optional.of(ONE_DAY);
		}
		for (Expiration expiration : values()) {
			if (expiration.code.equalsIgnoreCase(code.trim())) {
				return Optional.of(expiration);
			}
		}
		return Optional.empty();
	}
}
