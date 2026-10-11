import { expect, test } from "../fixtures";

test.describe("filter and sort", () => {
  test("priority view groups tasks, and the priority filter narrows them", async ({ tasks, api }) => {
    const tag = api.uniq("Triage");
    await api.createTask({ title: `${tag} low`, priority: "LOW" });
    await api.createTask({ title: `${tag} urgent`, priority: "URGENT" });
    await api.createTask({ title: `${tag} medium`, priority: "MEDIUM" });
    await tasks.goto();
    await tasks.search(tag);

    await expect(tasks.task(`${tag} urgent`)).toBeVisible();
    expect(await tasks.titles()).toEqual([`${tag} urgent`, `${tag} medium`, `${tag} low`]);

    await tasks.filterPriority("Low");
    await expect(tasks.task(`${tag} low`)).toBeVisible();
    await expect(tasks.task(`${tag} urgent`)).toHaveCount(0);
    await expect(tasks.task(`${tag} medium`)).toHaveCount(0);
  });

  test("sort by title and by due date", async ({ tasks, api }) => {
    const tag = api.uniq("Order");
    await api.createTask({ title: `${tag} b`, dueDate: "2999-01-02" });
    await api.createTask({ title: `${tag} c`, dueDate: "2999-01-01" });
    await api.createTask({ title: `${tag} a` }); // no due date: sorts last
    await tasks.goto();
    await tasks.search(tag);
    await expect(tasks.task(`${tag} a`)).toBeVisible();

    await tasks.sortBy("Title");
    await expect.poll(() => tasks.titles()).toEqual([`${tag} a`, `${tag} b`, `${tag} c`]);

    await tasks.sortBy("Due date");
    await expect.poll(() => tasks.titles()).toEqual([`${tag} c`, `${tag} b`, `${tag} a`]);
  });

  test("search matches title text only for the term typed", async ({ tasks, api }) => {
    const tag = api.uniq("Needle");
    await api.createTask({ title: `${tag} wanted` });
    await api.createTask({ title: api.uniq("Haystack") });
    await tasks.goto();

    await tasks.search(tag);

    await expect(tasks.task(`${tag} wanted`)).toBeVisible();
    expect(await tasks.titles()).toEqual([`${tag} wanted`]);
  });
});
