import { expect, test } from "../fixtures";

test.describe("categories", () => {
  test("seeded categories are listed", async ({ tasks }) => {
    await tasks.goto();
    for (const name of ["Personal", "Work", "Home", "Shopping", "Ideas"]) {
      await expect(tasks.category(name)).toBeVisible();
    }
  });

  test("create a category, add a task to it, rename it", async ({ tasks, api }) => {
    const name = api.uniq("Garden");
    const renamed = `${name}2`;
    const title = api.uniq("Plant tomatoes");
    await tasks.goto();

    await tasks.addCategory(name);
    await tasks.selectCategory(name);
    await tasks.quickAdd(title); // preselects the category chosen in the sidebar
    await expect(tasks.task(title)).toBeVisible();
    await expect(tasks.category(name)).toContainText("1");

    await tasks.renameCategory(name, renamed);
    await tasks.selectCategory(renamed);
    await expect(tasks.task(title)).toBeVisible();
  });

  test("selecting a category shows only its tasks", async ({ tasks, api }) => {
    const category = await api.createCategory(api.uniq("Errands"));
    const inside = api.uniq("Post letter");
    const outside = api.uniq("Elsewhere");
    await api.createTask({ title: inside, categoryId: category.id });
    await api.createTask({ title: outside });
    await tasks.goto();

    await tasks.selectCategory(category.name);

    await expect(tasks.task(inside)).toBeVisible();
    await expect(tasks.task(outside)).toHaveCount(0);
  });

  test("deleting a category moves its tasks to Inbox", async ({ tasks, api }) => {
    const category = await api.createCategory(api.uniq("Temp"));
    const title = api.uniq("Orphan");
    await api.createTask({ title, categoryId: category.id });
    await tasks.goto();

    await tasks.deleteCategory(category.name);
    await tasks.selectCategory("Inbox");
    await tasks.search(title);

    await expect(tasks.task(title)).toBeVisible();
    const categories = await api.listCategories();
    expect(categories.map((c) => c.name)).not.toContain(category.name);
  });
});
