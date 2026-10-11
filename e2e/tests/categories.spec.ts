import { expect, test } from "../fixtures";

test.describe("categories", () => {
  test("a new account starts with Personal, Ideas and Work", async ({ tasks }) => {
    await tasks.goto();

    await expect.poll(() => tasks.categoryOrder()).toEqual(["Personal", "Ideas", "Work"]);
    await expect(tasks.category("Shopping")).toHaveCount(0);
  });

  test("Home shows the tasks of every category and of Inbox", async ({ tasks, api }) => {
    const first = await api.createCategory(api.uniq("Alpha"));
    const second = await api.createCategory(api.uniq("Beta"));
    const tag = api.uniq("Everywhere");
    await api.createTask({ title: `${tag} a`, categoryId: first.id });
    await api.createTask({ title: `${tag} b`, categoryId: second.id });
    await api.createTask({ title: `${tag} c` });
    await tasks.goto();
    await tasks.selectCategory(first.name);
    await tasks.search(tag);
    await expect(tasks.task(`${tag} a`)).toBeVisible();
    await expect(tasks.task(`${tag} b`)).toHaveCount(0);

    await tasks.selectCategory("Home");

    await expect(tasks.task(`${tag} b`)).toBeVisible();
    expect((await tasks.titles()).sort()).toEqual([`${tag} a`, `${tag} b`, `${tag} c`]);
  });

  test("categories are reordered with the keyboard and the order persists", async ({ tasks, api }) => {
    await tasks.goto();
    await expect.poll(() => tasks.categoryOrder()).toEqual(["Personal", "Ideas", "Work"]);

    await tasks.moveCategory("Work", -1);

    await expect.poll(() => tasks.categoryOrder()).toEqual(["Personal", "Work", "Ideas"]);
    await expect.poll(async () => (await api.listCategories()).map((c) => c.name)).toEqual(["Personal", "Work", "Ideas"]);
    await tasks.page.reload();
    await expect.poll(() => tasks.categoryOrder()).toEqual(["Personal", "Work", "Ideas"]);
  });

  test("categories are reordered by dragging", async ({ tasks, api, isMobile }) => {
    test.skip(isMobile, "the pointer drag is exercised on the desktop layout; touch uses the same sensor");
    await tasks.goto();
    await expect.poll(() => tasks.categoryOrder()).toEqual(["Personal", "Ideas", "Work"]);

    await tasks.dragCategory("Personal", "Work");

    await expect.poll(() => tasks.categoryOrder()).toEqual(["Ideas", "Work", "Personal"]);
    await expect.poll(async () => (await api.listCategories()).map((c) => c.name)).toEqual(["Ideas", "Work", "Personal"]);
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
