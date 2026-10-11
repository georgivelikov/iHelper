package com.ihelper.task;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.util.Map;
import org.junit.jupiter.api.Test;

import com.ihelper.AbstractIT;

/** Delete moves a task to the bin; the bin can be restored from or emptied (the final deletion). */
class TaskBinIT extends AbstractIT {

	@Test
	void deletedTaskLeavesTheListAndShowsUpInTheBin() {
		String title = unique("binned");
		String id = createTask(Map.of("title", title));

		api().delete("/api/v1/tasks/" + id).then().statusCode(204);

		api().queryParam("q", title).get("/api/v1/tasks").then().body("items", empty());
		api().get("/api/v1/tasks/" + id).then().statusCode(404);
		api().get("/api/v1/tasks/bin").then().statusCode(200)
			.body("total", equalTo(1))
			.body("items[0].id", equalTo(id))
			.body("items[0].title", equalTo(title))
			.body("items[0].deletedAt", notNullValue());
		api().get("/api/v1/tasks/counts").then().body("bin", equalTo(1));
	}

	@Test
	void binListsTheMostRecentlyDeletedFirst() throws InterruptedException {
		String first = createTask(Map.of("title", unique("first")));
		String second = createTask(Map.of("title", unique("second")));
		api().delete("/api/v1/tasks/" + first).then().statusCode(204);
		Thread.sleep(20);
		api().delete("/api/v1/tasks/" + second).then().statusCode(204);

		api().get("/api/v1/tasks/bin").then().statusCode(200).body("items.id", contains(second, first));
	}

	@Test
	void restoreBringsTheTaskBackWithItsData() {
		String cat = createCategory(unique("keep"));
		String title = unique("restore");
		String id = createTask(Map.of("title", title, "categoryId", cat, "priority", "HIGH"));
		api().delete("/api/v1/tasks/" + id).then().statusCode(204);

		api().post("/api/v1/tasks/" + id + "/restore").then().statusCode(200)
			.body("title", equalTo(title))
			.body("priority", equalTo("HIGH"))
			.body("categoryId", equalTo(cat))
			.body("deletedAt", nullValue());

		api().get("/api/v1/tasks/" + id).then().statusCode(200);
		api().get("/api/v1/tasks/bin").then().body("items", empty());
		api().get("/api/v1/tasks/counts").then().body("bin", equalTo(0));
		// Restoring something that is not in the bin is harmless.
		api().post("/api/v1/tasks/" + id + "/restore").then().statusCode(200);
	}

	@Test
	void restoredTaskWhoseCategoryWasDeletedGoesToInbox() {
		String cat = createCategory(unique("gone"));
		String id = createTask(Map.of("title", unique("orphan"), "categoryId", cat));
		api().delete("/api/v1/tasks/" + id).then().statusCode(204);
		api().delete("/api/v1/categories/" + cat).then().statusCode(204);

		api().post("/api/v1/tasks/" + id + "/restore").then().statusCode(200).body("categoryId", nullValue());
	}

	@Test
	void restoreWithStaleVersionIs409() {
		String id = createTask(Map.of("title", unique("stale")));
		api().delete("/api/v1/tasks/" + id).then().statusCode(204);

		api().header("If-Match", "\"999\"").post("/api/v1/tasks/" + id + "/restore").then().statusCode(409);
	}

	@Test
	void emptyingTheBinErasesContentButNotTheTombstone() throws InterruptedException {
		String title = unique("secret-title");
		String keep = createTask(Map.of("title", unique("keep")));
		String gone = createTask(Map.of("title", title, "description", "private details"));
		api().delete("/api/v1/tasks/" + gone).then().statusCode(204);
		String checkpoint = api().get("/api/v1/sync").then().extract().path("serverTime");
		Thread.sleep(20);

		api().delete("/api/v1/tasks/bin").then().statusCode(200).body("purged", equalTo(1));

		api().get("/api/v1/tasks/bin").then().body("items", empty());
		api().get("/api/v1/tasks/counts").then().body("bin", equalTo(0));
		api().get("/api/v1/tasks/" + keep).then().statusCode(200);
		api().post("/api/v1/tasks/" + gone + "/restore").then().statusCode(404);
		// Other devices still learn about it, but the content is gone.
		api().queryParam("since", checkpoint).get("/api/v1/sync").then().statusCode(200)
			.body("tasks", hasSize(1))
			.body("tasks[0].id", equalTo(gone))
			.body("tasks[0].deletedAt", notNullValue())
			.body("tasks[0].title", equalTo(""))
			.body("tasks[0].description", nullValue());
		api().delete("/api/v1/tasks/bin").then().statusCode(200).body("purged", equalTo(0));
	}

	@Test
	void theBinBelongsToTheUser() {
		String mine = createTask(Map.of("title", unique("mine")));
		api().delete("/api/v1/tasks/" + mine).then().statusCode(204);
		Session other = register(unique("other"));

		as(other).get("/api/v1/tasks/bin").then().statusCode(200).body("items", empty());
		as(other).post("/api/v1/tasks/" + mine + "/restore").then().statusCode(404);
		as(other).delete("/api/v1/tasks/bin").then().statusCode(200).body("purged", equalTo(0));

		api().get("/api/v1/tasks/bin").then().body("items.id", hasItem(mine));
	}

	@Test
	void binRejectsBadPaging() {
		api().queryParam("size", 0).get("/api/v1/tasks/bin").then().statusCode(400);
		api().queryParam("page", -1).get("/api/v1/tasks/bin").then().statusCode(400);
	}

}
