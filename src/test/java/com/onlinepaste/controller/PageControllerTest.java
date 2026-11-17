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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "onlinepaste.rate-limit.enabled=false")
@AutoConfigureMockMvc
@Import(TestStoreConfig.class)
class PageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void homePageRenders() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Create share link")))
				.andExpect(content().string(containsString("maxlength=\"100000\"")));
	}

	@Test
	void helpPageRenders() throws Exception {
		mockMvc.perform(get("/help"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("How it works")));
	}

	@Test
	void viewPageEscapesPasteContent() throws Exception {
		String payload = objectMapper.writeValueAsString(java.util.Map.of(
				"content", "<script>alert('xss')</script>\n\n**bold**",
				"expiration", "1d"));
		String body = mockMvc.perform(post("/api/pastes")
						.contentType(MediaType.APPLICATION_JSON)
						.content(payload))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(body);

		mockMvc.perform(get("/p/{id}", json.get("id").asText()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("&lt;script&gt;alert")))
				.andExpect(content().string(not(containsString("<script>alert('xss')</script>"))))
				.andExpect(content().string(containsString("**bold**")));
	}

	@Test
	void missingPasteRendersNotFoundPage() throws Exception {
		mockMvc.perform(get("/p/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(content().string(containsString("This paste is gone")));
	}
}
