package com.ihelper.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ihelper.common.ConflictException;
import com.ihelper.common.Created;

// Security is covered by the *IT classes; these slices test controller mapping and validation only.
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

	private static final Instant T = Instant.parse("2026-03-01T10:00:00Z");

	@Autowired
	MockMvc mvc;

	@MockitoBean
	CategoryService service;

	@Test
	void listReturnsPlainArray() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.list()).thenReturn(List.of(new CategoryDto(id, "Work", "#0ea5e9", T, T, 0L, null)));

		mvc.perform(get("/api/v1/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("Work"));
	}

	@Test
	void invalidColorIsFieldError() throws Exception {
		mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Work\",\"color\":\"red\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("color"));
	}

	@Test
	void duplicateNameIs409() throws Exception {
		when(service.create(any())).thenThrow(new ConflictException("exists"));

		mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Work\"}"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	void createReturns201() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.create(any()))
			.thenReturn(new Created<>(new CategoryDto(id, "Gym", null, T, T, 0L, null), true));

		mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Gym\"}"))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/categories/" + id));
	}

	@Test
	void corsAllowsConfiguredOriginOnly() throws Exception {
		mvc.perform(options("/api/v1/categories").header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "PATCH"))
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

		mvc.perform(options("/api/v1/categories").header("Origin", "http://evil.example")
				.header("Access-Control-Request-Method", "GET"))
			.andExpect(status().isForbidden());
	}

}
