package com.ihelper.task;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ihelper.AbstractIT;

class TaskApiIT extends AbstractIT {

	private static Map<String, Object> task(String title) {
		Map<String, Object> body = new HashMap<>();
		body.put("title", title);
		return body;
	}

	@Test
	void createAppliesDefaultsAndGetReturnsIt() {
		String title = unique("defaults");
		String id = createTask(task(title));

		api().get("/api/v1/tasks/" + id).then().statusCode(200)
			.body("title", equalTo(title))
			.body("priority", equalTo("MEDIUM"))
			.body("status", equalTo("TODO"))
			.body("categoryId", nullValue())
			.body("version", equalTo(0))
			.body("createdAt", notNullValue())
			.body("deletedAt", nullValue());
	}

	@Test
	void searchMatchesTitleAndNotesIgnoringCase() {
		String needle = unique("needle");
		String inTitle = createTask(task("About " + needle));
		Map<String, Object> withNotes = task(unique("plain title"));
		withNotes.put("notes", "Remember the " + needle.toUpperCase() + " details");
		String inNotes = createTask(withNotes);
		createTask(task(unique("unrelated")));

		api().queryParam("q", needle).get("/api/v1/tasks").then().statusCode(200)
			.body("total", equalTo(2))
			.body("items.id", containsInAnyOrder(inTitle, inNotes));
	}

	@Test
	void postWithSameClientUuidIsIdempotent() {
		String id = UUID.randomUUID().toString();
		String title = unique("idem");
		Map<String, Object> body = task(title);
		body.put("id", id);

		api().body(body).post("/api/v1/tasks").then().statusCode(201).body("id", equalTo(id));
		api().body(body).post("/api/v1/tasks").then().statusCode(200).body("id", equalTo(id));

		api().queryParam("q", title).get("/api/v1/tasks").then().statusCode(200)
			.body("total", equalTo(1))
			.body("items", hasSize(1));
	}

	@Test
	void putWithStaleVersionIs409WithCurrentServerCopy() {
		String id = createTask(task(unique("stale")));
		Map<String, Object> update = task("renamed once");
		update.put("priority", "HIGH");
		update.put("status", "TODO");
		update.put("version", 0);
		api().body(update).put("/api/v1/tasks/" + id).then().statusCode(200).body("version", equalTo(1));

		update.put("title", "renamed twice, from a stale copy");
		api().body(update).put("/api/v1/tasks/" + id).then().statusCode(409)
			.contentType("application/problem+json")
			.body("status", equalTo(409))
			.body("current.version", equalTo(1))
			.body("current.title", equalTo("renamed once"));

		api().get("/api/v1/tasks/" + id).then().body("title", equalTo("renamed once"));
	}

	@Test
	void ifMatchHeaderIsAcceptedInPlaceOfBodyVersion() {
		String id = createTask(task(unique("etag")));
		Map<String, Object> update = task("via header");
		update.put("priority", "LOW");
		update.put("status", "TODO");

		api().header("If-Match", "\"0\"").body(update).put("/api/v1/tasks/" + id).then().statusCode(200)
			.body("version", equalTo(1));
		api().header("If-Match", "\"0\"").body(update).put("/api/v1/tasks/" + id).then().statusCode(409);
	}

	@Test
	void putWithoutVersionIs400() {
		String id = createTask(task(unique("noversion")));
		Map<String, Object> update = task("x");
		update.put("priority", "LOW");
		update.put("status", "TODO");

		api().body(update).put("/api/v1/tasks/" + id).then().statusCode(400)
			.contentType("application/problem+json");
	}

	@Test
	void patchMarksDoneAndStampsCompletedAtThenReopenClearsIt() {
		String id = createTask(task(unique("patch")));

		api().body(Map.of("status", "DONE")).patch("/api/v1/tasks/" + id).then().statusCode(200)
			.body("status", equalTo("DONE"))
			.body("completedAt", notNullValue())
			.body("version", equalTo(1));

		api().body(Map.of("status", "IN_PROGRESS")).patch("/api/v1/tasks/" + id).then().statusCode(200)
			.body("completedAt", nullValue());
	}

	@Test
	void patchExplicitNullClearsAndAbsentFieldsAreUntouched() {
		Map<String, Object> body = task(unique("clear"));
		body.put("notes", "details");
		body.put("dueDate", "2030-01-15");
		body.put("priority", "URGENT");
		String id = createTask(body);

		api().body("{\"dueDate\":null}").patch("/api/v1/tasks/" + id).then().statusCode(200)
			.body("dueDate", nullValue())
			.body("notes", equalTo("details"))
			.body("priority", equalTo("URGENT"));
	}

	@Test
	void patchWithStaleVersionConflicts() {
		String id = createTask(task(unique("patchstale")));
		api().body(Map.of("priority", "HIGH")).patch("/api/v1/tasks/" + id).then().statusCode(200);

		api().body(Map.of("priority", "LOW", "version", 0)).patch("/api/v1/tasks/" + id).then().statusCode(409)
			.body("current.priority", equalTo("HIGH"));
	}

	@Test
	void validationErrorsAreProblemDetailsWithFieldErrors() {
		api().body(Map.of("title", "", "notes", "ok")).post("/api/v1/tasks").then().statusCode(400)
			.contentType("application/problem+json")
			.body("title", equalTo("Validation failed"))
			.body("errors.field", hasItem("title"));
	}

	@Test
	void unknownCategoryIsRejected() {
		Map<String, Object> body = task(unique("nocat"));
		body.put("categoryId", UUID.randomUUID().toString());

		api().body(body).post("/api/v1/tasks").then().statusCode(400).contentType("application/problem+json");
	}

	@Test
	void unknownTaskIs404ProblemDetails() {
		api().get("/api/v1/tasks/" + UUID.randomUUID()).then().statusCode(404)
			.contentType("application/problem+json")
			.body("status", equalTo(404));
	}

	@Test
	void deleteIsSoftIdempotentAndHidesTheTask() {
		String id = createTask(task(unique("del")));

		api().delete("/api/v1/tasks/" + id).then().statusCode(204);
		api().delete("/api/v1/tasks/" + id).then().statusCode(204);
		api().get("/api/v1/tasks/" + id).then().statusCode(404);
		api().body(Map.of("title", "x", "priority", "LOW", "status", "TODO", "version", 1))
			.put("/api/v1/tasks/" + id).then().statusCode(404);
	}

	@Test
	void deletingUnknownTaskIs404() {
		api().delete("/api/v1/tasks/" + UUID.randomUUID()).then().statusCode(404);
	}

	@Test
	void filtersCombineAndSortByPriorityThenDueDate() {
		String marker = unique("flt");
		String cat = createCategory(unique("cat"));
		createTask(Map.of("title", marker + " a", "priority", "LOW", "categoryId", cat));
		createTask(Map.of("title", marker + " b", "priority", "URGENT", "categoryId", cat, "dueDate", "2031-06-01"));
		createTask(Map.of("title", marker + " c", "priority", "URGENT", "categoryId", cat, "dueDate", "2031-01-01"));
		createTask(Map.of("title", marker + " d", "priority", "HIGH", "status", "DONE"));

		api().queryParam("q", marker).queryParam("sort", "-priority,dueDate").get("/api/v1/tasks").then()
			.statusCode(200)
			.body("items.title", contains(marker + " c", marker + " b", marker + " d", marker + " a"));
		api().queryParam("q", marker).queryParam("categoryId", cat).get("/api/v1/tasks").then()
			.body("total", equalTo(3));
		api().queryParam("q", marker).queryParam("priority", "URGENT").get("/api/v1/tasks").then()
			.body("items.title", containsInAnyOrder(marker + " b", marker + " c"));
		api().queryParam("q", marker).queryParam("status", "DONE").get("/api/v1/tasks").then()
			.body("items.title", contains(marker + " d"));
		api().queryParam("q", marker).queryParam("inbox", true).get("/api/v1/tasks").then()
			.body("items.title", contains(marker + " d"));
	}

	@Test
	void paginationEnvelope() {
		String marker = unique("pg");
		for (int i = 0; i < 5; i++) {
			createTask(Map.of("title", marker + " " + i));
		}

		api().queryParam("q", marker).queryParam("sort", "title").queryParam("page", 2).queryParam("size", 2)
			.get("/api/v1/tasks").then().statusCode(200)
			.body("page", equalTo(2))
			.body("size", equalTo(2))
			.body("total", equalTo(5))
			.body("items.title", contains(marker + " 4"));
		api().queryParam("q", marker).queryParam("page", 9).get("/api/v1/tasks").then().statusCode(200)
			.body("items", empty());
	}

	@Test
	void badQueryParametersAre400ProblemDetails() {
		api().queryParam("size", 100000).get("/api/v1/tasks").then().statusCode(400)
			.contentType("application/problem+json");
		api().queryParam("sort", "bogus").get("/api/v1/tasks").then().statusCode(400);
		api().queryParam("priority", "SOON").get("/api/v1/tasks").then().statusCode(400);
	}

	@Test
	void malformedJsonIs400() {
		api().body("{not json").post("/api/v1/tasks").then().statusCode(400)
			.contentType("application/problem+json")
			.body("detail", notNullValue());
	}

	@Test
	void statusFilterAcceptsSeveralValues() {
		String marker = unique("multi");
		createTask(Map.of("title", marker + " todo", "status", "TODO"));
		createTask(Map.of("title", marker + " doing", "status", "IN_PROGRESS"));
		createTask(Map.of("title", marker + " done", "status", "DONE"));

		api().queryParam("q", marker).queryParam("status", "TODO", "IN_PROGRESS").queryParam("sort", "title")
			.get("/api/v1/tasks").then().statusCode(200)
			.body("items.title", contains(marker + " doing", marker + " todo"));
	}

	@Test
	void countsReportOpenTasksPerCategoryAndInbox() {
		String cat = createCategory(unique("counted"));
		createTask(Map.of("title", unique("c1"), "categoryId", cat));
		createTask(Map.of("title", unique("c2"), "categoryId", cat, "status", "IN_PROGRESS"));
		createTask(Map.of("title", unique("c3"), "categoryId", cat, "status", "DONE"));
		createTask(Map.of("title", unique("inbox")));

		api().get("/api/v1/tasks/counts").then().statusCode(200)
			.body("byCategory." + cat, equalTo(2))
			.body("inbox", org.hamcrest.Matchers.greaterThanOrEqualTo(1))
			.body("total", org.hamcrest.Matchers.greaterThanOrEqualTo(3));
	}

}
