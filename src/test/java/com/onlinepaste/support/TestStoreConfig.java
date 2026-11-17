package com.onlinepaste.support;

import com.onlinepaste.service.PasteStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestStoreConfig {

	@Bean
	@Primary
	public PasteStore inMemoryPasteStore() {
		return new InMemoryPasteStore();
	}
}
