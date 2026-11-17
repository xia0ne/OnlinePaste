package com.onlinepaste.config;

import com.onlinepaste.web.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final OnlinePasteProperties properties;

	public WebConfig(OnlinePasteProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new RateLimitInterceptor(properties.rateLimit()))
				.addPathPatterns("/api/**");
	}
}
