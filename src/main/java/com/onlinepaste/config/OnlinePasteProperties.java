package com.onlinepaste.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Application settings. Every value can be overridden through an environment
 * variable, see application.yml for the mapping.
 */
@Validated
@ConfigurationProperties(prefix = "onlinepaste")
public record OnlinePasteProperties(
		@DefaultValue("http://localhost:8080") String baseUrl,
		@DefaultValue("100000") @Min(1) int maxContentLength,
		@DefaultValue("10") @Min(6) @Max(64) int idLength,
		@DefaultValue("true") boolean viewCountEnabled,
		@DefaultValue RateLimit rateLimit) {

	public record RateLimit(
			@DefaultValue("true") boolean enabled,
			@DefaultValue("30") @Min(1) int permitsPerMinute) {
	}
}
