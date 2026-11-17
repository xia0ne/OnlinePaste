package com.onlinepaste.web;

import com.onlinepaste.config.OnlinePasteProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window, per-client rate limit for write requests. It intentionally
 * keeps state in memory: a single instance is the normal deployment shape and
 * a restart only resets the current minute.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

	private static final long WINDOW_MILLIS = 60_000L;
	private static final int CLEANUP_THRESHOLD = 10_000;

	private final boolean enabled;
	private final int permitsPerMinute;
	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	public RateLimitInterceptor(OnlinePasteProperties.RateLimit rateLimit) {
		this.enabled = rateLimit.enabled();
		this.permitsPerMinute = rateLimit.permitsPerMinute();
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws IOException {
		if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		long now = System.currentTimeMillis();
		if (windows.size() > CLEANUP_THRESHOLD) {
			windows.entrySet().removeIf(entry -> now - entry.getValue().start >= WINDOW_MILLIS);
		}

		String client = request.getRemoteAddr();
		Window window = windows.compute(client, (key, current) -> {
			if (current == null || now - current.start >= WINDOW_MILLIS) {
				return new Window(now);
			}
			return current;
		});

		if (window.count.incrementAndGet() > permitsPerMinute) {
			long retryAfter = Math.max(1, (WINDOW_MILLIS - (now - window.start) + 999) / 1000);
			response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.getWriter().write(
					"{\"error\":\"rate_limited\",\"message\":\"Too many requests, please retry later.\"}");
			return false;
		}
		return true;
	}

	private static final class Window {
		private final long start;
		private final AtomicInteger count = new AtomicInteger();

		private Window(long start) {
			this.start = start;
		}
	}
}
