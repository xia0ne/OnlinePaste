package com.onlinepaste.web;

import com.onlinepaste.config.OnlinePasteProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitInterceptorTest {

	private final OnlinePasteProperties.RateLimit enabled =
			new OnlinePasteProperties.RateLimit(true, 2);

	@Test
	void blocksRequestsAboveTheLimit() throws Exception {
		RateLimitInterceptor interceptor = new RateLimitInterceptor(enabled);

		assertThat(interceptor.preHandle(request("POST"), new MockHttpServletResponse(), new Object())).isTrue();
		assertThat(interceptor.preHandle(request("POST"), new MockHttpServletResponse(), new Object())).isTrue();

		MockHttpServletResponse blocked = new MockHttpServletResponse();
		assertThat(interceptor.preHandle(request("POST"), blocked, new Object())).isFalse();
		assertThat(blocked.getStatus()).isEqualTo(429);
		assertThat(blocked.getHeader("Retry-After")).isNotBlank();
		assertThat(blocked.getContentAsString()).contains("rate_limited");
	}

	@Test
	void readRequestsAreNotLimited() throws Exception {
		RateLimitInterceptor interceptor = new RateLimitInterceptor(enabled);

		for (int i = 0; i < 10; i++) {
			assertThat(interceptor.preHandle(request("GET"), new MockHttpServletResponse(), new Object())).isTrue();
		}
	}

	@Test
	void disabledLimitAllowsEverything() throws Exception {
		RateLimitInterceptor interceptor = new RateLimitInterceptor(
				new OnlinePasteProperties.RateLimit(false, 1));

		for (int i = 0; i < 10; i++) {
			assertThat(interceptor.preHandle(request("POST"), new MockHttpServletResponse(), new Object())).isTrue();
		}
	}

	private MockHttpServletRequest request(String method) {
		return new MockHttpServletRequest(method, "/api/pastes");
	}
}
