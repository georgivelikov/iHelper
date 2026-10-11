import { expect, test } from "../fixtures";

test.describe("tasks", () => {
  test("create a task with quick-add", async ({ tasks, api }) => {
    const title = api.uniq("Book flights");
    await tasks.goto();
    await tasks.search(title);

    await tasks.quickAdd(title, { priority: "High" });

    await expect(tasks.task(title)).toBeVisible();
    await expect(tasks.page.getByRole("region", { name: "High priority" })).toContainText(title);
    const stored = await api.listTasks(title);
    expect(stored).toHaveLength(1);
  });

  test("edit title, priority, category and due date", async ({ tasks, api }) => {
    const title = api.uniq("Draft report");
    const renamed = `${title} v2`;
    await api.createTask({ title, priority: "LOW" });
    await tasks.goto();
    await tasks.search(title);

    await tasks.openEditor(title);
    await tasks.editTask({ title: renamed, priority: "Urgent", category: "Work", dueDate: "2999-12-31", description: "needs numbers" });

    await expect(tasks.task(renamed)).toBeVisible();
    await expect(tasks.page.getByRole("region", { name: "Urgent priority" })).toContainText(renamed);
    await expect(tasks.page.getByText("Due 2999-12-31")).toBeVisible();

    const [stored] = await api.listTasks(title);
    expect(stored.title).toBe(renamed);
  });

  test("complete a task, find it under Done, and reopen it", async ({ tasks, api }) => {
    const title = api.uniq("Pay invoice");
    await api.createTask({ title });
    await tasks.goto();
    await tasks.search(title);

    await tasks.complete(title);
    await expect(tasks.task(title)).toHaveCount(0); // done tasks leave the open list

    await tasks.show("Done");
    await expect(tasks.task(title)).toBeVisible();
    await tasks.reopen(title);
    await expect(tasks.task(title)).toHaveCount(0);

    await tasks.show("Open");
    await expect(tasks.task(title)).toBeVisible();
  });

  test("delete a task from the editor", async ({ tasks, api }) => {
    const title = api.uniq("Throw away");
    await api.createTask({ title });
    await tasks.goto();
    await tasks.search(title);

    await tasks.openEditor(title);
    await tasks.deleteFromEditor();

    await expect(tasks.task(title)).toHaveCount(0);
    expect(await api.listTasks(title)).toHaveLength(0);
  });

  test("data survives a reload", async ({ tasks, api, page }) => {
    const title = api.uniq("Persist me");
    await tasks.goto();
    await tasks.quickAdd(title);
    await tasks.search(title);
    await expect(tasks.task(title)).toBeVisible();

    await page.reload();
    await tasks.search(title);

    await expect(tasks.task(title)).toBeVisible();
  });
});
