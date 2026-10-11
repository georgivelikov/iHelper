package com.ihelper.auth;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ihelper.AbstractIT;

/** One user must never see, change, delete or learn about another user's data. */
class UserIsolationIT extends AbstractIT {

	private Session other;

	@BeforeEach
	void registerOtherUser() {
		other = register(unique("other").substring(0, 30));
	}

	@Test
	void listsOnlyContainTheCallersOwnRows() {
		String mine = unique("mine");
		createTask(Map.of("title", mine));
		String theirs = unique("theirs");
		as(other).body(Map.of("title", theirs)).post("/api/v1/tasks").then().statusCode(201);

		api().get("/api/v1/tasks?size=200").then().statusCode(200)
			.body("items.title", hasItem(mine))
			.body("items.title", not(hasItem(theirs)));
		as(other).get("/api/v1/tasks?size=200").then().statusCode(200)
			.body("items.title", hasItem(theirs))
			.body("items.title", not(hasItem(mine)));
	}

	@Test
	void anotherUsersTaskIs404ForEveryOperation() {
		String id = createTask(Map.of("title", unique("private")));
		var path = "/api/v1/tasks/" + id;

		as(other).get(path).then().statusCode(404);
		as(other).body(Map.of("title", "x", "priority", "LOW", "status", "TODO", "version", 0)).put(path).then()
			.statusCode(404);
		as(other).body(Map.of("title", "hijack", "version", 0)).patch(path).then().statusCode(404);
		as(other).delete(path).then().statusCode(404);

		api().get(path).then().statusCode(200).body("deletedAt", equalTo(null));
	}

	@Test
	void anotherUsersCategoryIs404AndCannotBeUsedForATask() {
		String categoryId = createCategory(unique("private-cat"));
		var path = "/api/v1/categories/" + categoryId;

		as(other).get(path).then().statusCode(404);
		as(other).body(Map.of("name", "stolen", "version", 0)).put(path).then().statusCode(404);
		as(other).delete(path).then().statusCode(404);
		as(other).body(Map.of("title", "t", "categoryId", categoryId)).post("/api/v1/tasks").then().statusCode(400);

		api().get(path).then().statusCode(200);
	}

	@Test
	void takenIdsDoNotLeakAcrossUsers() {
		UUID id = UUID.randomUUID();
		api().body(Map.of("id", id, "title", "first owner")).post("/api/v1/tasks").then().statusCode(201);

		// Looks like any id collision: no data and no hint that it belongs to someone else.
		as(other).body(Map.of("id", id, "title", "second")).post("/api/v1/tasks").then().statusCode(409)
			.body("detail", equalTo("Id " + id + " is already in use"))
			.body("current", equalTo(null));
		api().get("/api/v1/tasks/" + id).then().statusCode(200).body("title", equalTo("first owner"));
	}

	@Test
	void categoryNamesAreUniquePerUserNotGlobally() {
		String name = unique("Shared");
		createCategory(name);

		as(other).body(Map.of("name", name)).post("/api/v1/categories").then().statusCode(201);
		api().body(Map.of("name", name)).post("/api/v1/categories").then().statusCode(409);
	}

	@Test
	void countsAndSyncAreScoped() {
		createTask(Map.of("title", unique("counted")));

		api().get("/api/v1/tasks/counts").then().statusCode(200).body("total", equalTo(1));
		as(other).get("/api/v1/tasks/counts").then().statusCode(200).body("total", equalTo(0));

		as(other).get("/api/v1/sync").then().statusCode(200).body("tasks", empty());
		api().get("/api/v1/sync").then().statusCode(200).body("tasks", hasSize(1));
	}

	@Test
	void deletingACategoryOnlyMovesTheOwnersTasks() {
		String categoryId = createCategory(unique("cat"));
		String task = createTask(Map.of("title", unique("t"), "categoryId", categoryId));

		api().delete("/api/v1/categories/" + categoryId).then().statusCode(204);

		api().get("/api/v1/tasks/" + task).then().statusCode(200).body("categoryId", equalTo(null));
	}

}
