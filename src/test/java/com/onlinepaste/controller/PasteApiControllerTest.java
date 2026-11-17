package com.onlinepaste.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinepaste.support.TestStoreConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"onlinepaste.rate-limit.enabled=false",
		"onlinepaste.base-url=https://paste.example"
})
@AutoConfigureMockMvc
@Import(TestStoreConfig.class)
class PasteApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void createsAndReadsPaste() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/pastes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"# hello\",\"expiration\":\"1h\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Content-Type", MediaType.APPLICATION_JSON_VALUE))
				.andExpect(jsonPath("$.id").isNotEmpty())
				.andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("https://paste.example/p/")))
				.andExpect(jsonPath("$.permanent").value(false))
				.andReturn();

		String id = readJson(created).get("id").asText();

		mockMvc.perform(get("/api/pastes/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.content").value("# hello"))
				.andExpect(jsonPath("$.views").value(1));
	}

	@Test
	void createsPermanentPaste() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/pastes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"keep me\",\"expiration\":\"never\",\"id\":\"keep-me\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value("keep-me"))
				.andExpect(jsonPath("$.expiresAt").doesNotExist())
				.andExpect(jsonPath("$.permanent").value(true))
				.andReturn();

		assertThat(readJson(created).get("url").asText()).isEqualTo("https://paste.example/p/keep-me");
	}

	@Test
	void rejectsBlankContent() throws Exception {
		mockMvc.perform(post("/api/pastes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_error"));
	}

	@Test
	void rejectsUnknownExpiration() throws Exception {
		mockMvc.perform(post("/api/pastes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"hello\",\"expiration\":\"2w\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("validation_error"));
	}

	@Test
	void rejectsContentAboveConfiguredLimit() throws Exception {
		String content = "a".repeat(100_001);
		String body = objectMapper.writeValueAsString(
				java.util.Map.of("content", content, "expiration", "1d"));

		mockMvc.perform(post("/api/pastes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("100000")));
	}

	@Test
	void rejectsDuplicateCustomId() throws Exception {
		String body = "{\"content\":\"hello\",\"expiration\":\"1d\",\"id\":\"taken-id\"}";

		mockMvc.perform(post("/api/pastes").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/pastes").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("conflict"));
	}

	@Test
	void returnsNotFoundForUnknownPaste() throws Exception {
		mockMvc.perform(get("/api/pastes/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("not_found"));
	}

	@Test
	void returnsMalformedJsonError() throws Exception {
		mockMvc.perform(post("/api/pastes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("malformed_json"));
	}

	@Test
	void servesQrCodeAsSvg() throws Exception {
		MvcResult created = mockMvc.perform(post("/api/pastes")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"content\":\"qr me\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String id = readJson(created).get("id").asText();

		mockMvc.perform(get("/api/pastes/{id}/qr.svg", id))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("image/svg+xml")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("<svg")));
	}

	@Test
	void setsSecurityHeaders() throws Exception {
		mockMvc.perform(get("/api/pastes/does-not-exist"))
				.andExpect(header().string("X-Content-Type-Options", "nosniff"))
				.andExpect(header().string("X-Frame-Options", "DENY"))
				.andExpect(header().string("Content-Security-Policy",
						org.hamcrest.Matchers.containsString("default-src 'self'")));
	}

	private JsonNode readJson(MvcResult result) throws Exception {
		return objectMapper.readTree(result.getResponse().getContentAsString());
	}
}
