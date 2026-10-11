package com.ihelper.category;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.ihelper.AbstractIT;

class CategoryApiIT extends AbstractIT {

	@Test
	void seedCategoriesArePresent() {
		api().get("/api/v1/categories").then().statusCode(200)
			.body("name", hasItems("Personal", "Work", "Home", "Shopping", "Ideas"));
	}

	@Test
	void createRenameAndFetch() {
		String id = createCategory(unique("cat"));

		String renamed = unique("renamed");
		api().body(Map.of("name", renamed, "color", "#112233", "version", 0)).put("/api/v1/categories/" + id)
			.then().statusCode(200)
			.body("name", equalTo(renamed))
			.body("color", equalTo("#112233"))
			.body("version", equalTo(1));
		api().get("/api/v1/categories/" + id).then().statusCode(200).body("name", equalTo(renamed));
	}

	@Test
	void duplicateNameIs409IgnoringCase() {
		String name = unique("dup");
		createCategory(name);

		api().body(Map.of("name", name.toUpperCase())).post("/api/v1/categories").then().statusCode(409)
			.contentType("application/problem+json");
	}

	@Test
	void postWithSameClientUuidIsIdempotent() {
		String id = UUID.randomUUID().toString();
		Map<String, Object> body = Map.of("id", id, "name", unique("idem"));

		api().body(body).post("/api/v1/categories").then().statusCode(201);
		api().body(body).post("/api/v1/categories").then().statusCode(200).body("id", equalTo(id));
	}

	@Test
	void staleUpdateIs409WithCurrentCopy() {
		String id = createCategory(unique("stale"));
		api().body(Map.of("name", unique("v1"), "version", 0)).put("/api/v1/categories/" + id).then()
			.statusCode(200);

		api().body(Map.of("name", unique("v-stale"), "version", 0)).put("/api/v1/categories/" + id).then()
			.statusCode(409)
			.body("current.version", equalTo(1));
	}

	@Test
	void invalidColorIs400() {
		api().body(Map.of("name", unique("c"), "color", "blue")).post("/api/v1/categories").then().statusCode(400)
			.body("errors.field", hasItem("color"));
	}

	@Test
	void deletingACategoryMovesItsTasksToInbox() {
		String cat = createCategory(unique("doomed"));
		String task = createTask(Map.of("title", unique("orphan"), "categoryId", cat));

		api().delete("/api/v1/categories/" + cat).then().statusCode(204);

		api().get("/api/v1/tasks/" + task).then().statusCode(200).body("categoryId", nullValue());
		api().get("/api/v1/categories/" + cat).then().statusCode(404);
		api().get("/api/v1/categories").then().body("id", not(hasItem(cat)));
		api().delete("/api/v1/categories/" + cat).then().statusCode(204);
	}

	@Test
	void deletedCategoryNameCanBeReused() {
		String name = unique("reuse");
		String first = createCategory(name);
		api().delete("/api/v1/categories/" + first).then().statusCode(204);

		String second = createCategory(name);

		api().get("/api/v1/categories/" + second).then().statusCode(200).body("name", equalTo(name));
	}

}
