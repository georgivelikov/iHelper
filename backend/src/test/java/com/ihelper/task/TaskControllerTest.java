package com.ihelper.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ihelper.common.ConflictException;
import com.ihelper.common.Created;
import com.ihelper.common.NotFoundException;

@WebMvcTest(TaskController.class)
class TaskControllerTest {

	private static final Instant T = Instant.parse("2026-03-01T10:00:00Z");

	@Autowired
	MockMvc mvc;

	@MockitoBean
	TaskService service;

	private static TaskDto dto(UUID id, long version) {
		return new TaskDto(id, "Buy milk", null, null, Priority.HIGH, TaskStatus.TODO, null, null, T, T, version,
				null);
	}

	@Test
	void listReturnsPageEnvelopeAndPassesFilters() throws Exception {
		UUID id = UUID.randomUUID();
		var page = new PageImpl<>(List.of(dto(id, 0)), PageRequest.of(1, 5), 11);
		when(service.list(any(), eq("-priority"), eq(1), eq(5))).thenReturn(page);

		mvc.perform(get("/api/v1/tasks?priority=HIGH&status=TODO&q=milk&sort=-priority&page=1&size=5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].id").value(id.toString()))
			.andExpect(jsonPath("$.page").value(1))
			.andExpect(jsonPath("$.size").value(5))
			.andExpect(jsonPath("$.total").value(11));

		verify(service).list(
				new TaskFilter(null, false, Priority.HIGH, TaskStatus.TODO, "milk"), "-priority", 1, 5);
	}

	@Test
	void invalidEnumQueryParamIsProblemDetails400() throws Exception {
		mvc.perform(get("/api/v1/tasks?priority=NOPE"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void createReturns201WithLocation() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.create(any())).thenReturn(new Created<>(dto(id, 0), true));

		mvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"" + id + "\",\"title\":\"Buy milk\"}"))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", "/api/v1/tasks/" + id))
			.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	void createWithExistingIdReturns200() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.create(any())).thenReturn(new Created<>(dto(id, 3), false));

		mvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON)
				.content("{\"id\":\"" + id + "\",\"title\":\"Buy milk\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(3));
	}

	@Test
	void blankTitleGivesFieldLevelValidationErrors() throws Exception {
		mvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"  \"}"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.title").value("Validation failed"))
			.andExpect(jsonPath("$.errors[0].field").value("title"));
	}

	@Test
	void malformedJsonIs400ProblemDetails() throws Exception {
		mvc.perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content("{nope"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void unknownTaskIs404ProblemDetails() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.get(id)).thenThrow(new NotFoundException("Task", id));

		mvc.perform(get("/api/v1/tasks/" + id))
			.andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void notAUuidIs400() throws Exception {
		mvc.perform(get("/api/v1/tasks/not-a-uuid")).andExpect(status().isBadRequest());
	}

	@Test
	void putWithoutVersionIs400() throws Exception {
		mvc.perform(put("/api/v1/tasks/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"x\",\"priority\":\"LOW\",\"status\":\"TODO\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("version")));
	}

	@Test
	void staleVersionIs409WithCurrentCopy() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.update(eq(id), any(), eq(1L)))
			.thenThrow(new ConflictException("stale", dto(id, 4)));

		mvc.perform(put("/api/v1/tasks/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"x\",\"priority\":\"LOW\",\"status\":\"TODO\",\"version\":1}"))
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.current.version").value(4))
			.andExpect(jsonPath("$.current.id").value(id.toString()));
	}

	@Test
	void ifMatchHeaderSuppliesTheVersion() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.update(eq(id), any(), eq(2L))).thenReturn(dto(id, 3));

		mvc.perform(put("/api/v1/tasks/" + id).header("If-Match", "\"2\"").contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"x\",\"priority\":\"LOW\",\"status\":\"TODO\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.version").value(3));
	}

	@Test
	void patchRecordsWhichFieldsWerePresent() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.patch(eq(id), any(), isNull())).thenReturn(dto(id, 1));

		mvc.perform(patch("/api/v1/tasks/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"DONE\",\"dueDate\":null}"))
			.andExpect(status().isOk());

		var captor = org.mockito.ArgumentCaptor.forClass(TaskPatch.class);
		verify(service).patch(eq(id), captor.capture(), isNull());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().present())
			.containsExactlyInAnyOrder("status", "dueDate");
		org.assertj.core.api.Assertions.assertThat(captor.getValue().values().status()).isEqualTo(TaskStatus.DONE);
	}

	@Test
	void patchWithNonObjectBodyIs400() throws Exception {
		mvc.perform(patch("/api/v1/tasks/" + UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
				.content("[1,2]"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void deleteReturns204() throws Exception {
		mvc.perform(delete("/api/v1/tasks/" + UUID.randomUUID())).andExpect(status().isNoContent());
	}

}
