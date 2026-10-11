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

  test("completing a task marks it Done and keeps it in the list; reopen undoes it", async ({ tasks, api }) => {
    const title = api.uniq("Pay invoice");
    await api.createTask({ title });
    await tasks.goto();
    await tasks.search(title);

    await tasks.complete(title);

    await expect(tasks.page.getByRole("checkbox", { name: `Reopen ${title}` })).toBeChecked();
    await expect(tasks.task(title)).toBeVisible(); // not removed automatically
    expect((await api.listTasks(title))[0].status).toBe("DONE");

    await tasks.show("Open");
    await expect(tasks.task(title)).toHaveCount(0);
    await tasks.show("Done");
    await expect(tasks.task(title)).toBeVisible();

    await tasks.reopen(title);
    await expect(tasks.task(title)).toHaveCount(0);
    expect((await api.listTasks(title))[0].status).toBe("TODO");
  });

  test("finished tasks sink below the open ones", async ({ tasks, api }) => {
    const tag = api.uniq("Sink");
    await api.createTask({ title: `${tag} finished`, status: "DONE" });
    await api.createTask({ title: `${tag} open` });
    await tasks.goto();
    await tasks.search(tag);

    await expect(tasks.task(`${tag} open`)).toBeVisible();
    expect(await tasks.titles()).toEqual([`${tag} open`, `${tag} finished`]);
  });

  test("clicking a row expands it to show the description, and the edit icon opens the editor", async ({ tasks, api }) => {
    const title = api.uniq("Click row");
    const description = `Details for ${title}`;
    await api.createTask({ title, priority: "HIGH", description });
    await tasks.goto();
    await tasks.search(title);
    await expect(tasks.page.getByText(description)).toHaveCount(0);

    await tasks.page.getByRole("region", { name: "High priority" }).getByText("High", { exact: true }).last().click();

    await expect(tasks.page.getByText(description)).toBeVisible();
    await expect(tasks.dialog).toBeHidden();

    await tasks.task(title).click(); // the title toggles it again
    await expect(tasks.page.getByText(description)).toHaveCount(0);

    await tasks.openEditor(title);
    await expect(tasks.dialog.getByLabel("Title")).toHaveValue(title);
    await expect(tasks.dialog.getByLabel("Description")).toHaveValue(description);
  });

  test("delete icon moves a task to the bin, and it can be restored", async ({ tasks, api }) => {
    const title = api.uniq("Bin me");
    await api.createTask({ title });
    await tasks.goto();
    await tasks.search(title);

    await tasks.deleteTask(title);

    await expect(tasks.task(title)).toHaveCount(0);
    expect(await api.listTasks(title)).toHaveLength(0);
    await tasks.openBin();
    await expect(tasks.binTask(title)).toBeVisible();

    await tasks.restoreFromBin(title);
    await expect(tasks.binTask(title)).toHaveCount(0);
    await tasks.selectCategory("Home");
    await tasks.search(title);
    await expect(tasks.task(title)).toBeVisible();
  });

  test("selected tasks can be deleted from the bin while the others stay", async ({ tasks, api }) => {
    const tag = api.uniq("Bin");
    const erase = `${tag} erase`;
    const keep = `${tag} keep`;
    await api.createTask({ title: erase });
    await api.createTask({ title: keep });
    await tasks.goto();
    await tasks.search(tag);
    await tasks.deleteTask(erase);
    await tasks.deleteTask(keep);
    await tasks.openBin();
    await expect(tasks.binTask(erase)).toBeVisible();
    await expect(tasks.binTask(keep)).toBeVisible();

    await tasks.deleteSelectedFromBin(erase);

    await expect(tasks.binTask(erase)).toHaveCount(0);
    await expect(tasks.binTask(keep)).toBeVisible();
    await tasks.page.reload();
    await tasks.openBin();
    await expect(tasks.binTask(erase)).toHaveCount(0);
    await expect(tasks.binTask(keep)).toBeVisible();
    await tasks.restoreFromBin(keep);
    await tasks.selectCategory("Home");
    await tasks.search(keep);
    await expect(tasks.task(keep)).toBeVisible();
  });

  test("emptying the bin deletes its tasks for good", async ({ tasks, api }) => {
    const keep = api.uniq("Keep");
    const gone = api.uniq("Erase");
    await api.createTask({ title: keep });
    await api.createTask({ title: gone });
    await tasks.goto();
    await tasks.search(gone);
    await tasks.deleteTask(gone);
    await tasks.openBin();
    await expect(tasks.binTask(gone)).toBeVisible();

    await tasks.emptyBin();

    await expect(tasks.page.getByText("The bin is empty.")).toBeVisible();
    await tasks.page.reload();
    await tasks.openBin();
    await expect(tasks.page.getByText("The bin is empty.")).toBeVisible();
    expect(await api.listTasks(gone)).toHaveLength(0);
    expect(await api.listTasks(keep)).toHaveLength(1);
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
    await tasks.openBin();
    await expect(tasks.binTask(title)).toBeVisible(); // recoverable, not gone
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
