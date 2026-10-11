package com.ihelper.sync;

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

/** The "offline mobile client" scenario: poll /sync with the previous serverTime. */
class SyncApiIT extends AbstractIT {

	private static String serverTime() {
		return api().get("/api/v1/sync?since=2000-01-01T00:00:00Z").then().statusCode(200).extract()
			.path("serverTime");
	}

	@Test
	void reportsCreatesUpdatesAndDeletionsSinceTheCheckpoint() throws InterruptedException {
		String title = unique("sync");
		String task = createTask(Map.of("title", title));
		String checkpoint = serverTime();
		Thread.sleep(20);

		api().queryParam("since", checkpoint).get("/api/v1/sync").then().statusCode(200)
			.body("tasks", empty())
			.body("categories", empty());

		api().body(Map.of("priority", "HIGH")).patch("/api/v1/tasks/" + task).then().statusCode(200);
		api().queryParam("since", checkpoint).get("/api/v1/sync").then()
			.body("tasks.id", contains(task))
			.body("tasks[0].priority", equalTo("HIGH"))
			.body("tasks[0].deletedAt", nullValue());

		String afterUpdate = serverTime();
		Thread.sleep(20);
		api().delete("/api/v1/tasks/" + task).then().statusCode(204);

		api().queryParam("since", afterUpdate).get("/api/v1/sync").then()
			.body("tasks", hasSize(1))
			.body("tasks[0].id", equalTo(task))
			.body("tasks[0].deletedAt", notNullValue());
	}

	@Test
	void firstFullSyncLeavesOutDeletedRows() {
		String live = createTask(Map.of("title", unique("live")));
		String gone = createTask(Map.of("title", unique("gone")));
		api().delete("/api/v1/tasks/" + gone).then().statusCode(204);

		var tasks = api().queryParam("size", 1).get("/api/v1/sync").then().statusCode(200)
			.body("serverTime", notNullValue())
			.extract().<java.util.List<String>>path("tasks.id");

		org.assertj.core.api.Assertions.assertThat(tasks).contains(live).doesNotContain(gone);
	}

	@Test
	void categoryDeletionShowsUpAlongsideItsMovedTasks() throws InterruptedException {
		String cat = createCategory(unique("syncdel"));
		String task = createTask(Map.of("title", unique("moved"), "categoryId", cat));
		String checkpoint = serverTime();
		Thread.sleep(20);

		api().delete("/api/v1/categories/" + cat).then().statusCode(204);

		api().queryParam("since", checkpoint).get("/api/v1/sync").then().statusCode(200)
			.body("categories.id", hasItem(cat))
			.body("categories.find { it.id == '" + cat + "' }.deletedAt", notNullValue())
			.body("tasks.find { it.id == '" + task + "' }.categoryId", nullValue());
	}

	@Test
	void malformedSinceIs400() {
		api().queryParam("since", "yesterday").get("/api/v1/sync").then().statusCode(400)
			.contentType("application/problem+json");
	}

}
