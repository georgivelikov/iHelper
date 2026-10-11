import { expect, type Locator, type Page } from "@playwright/test";

type Priority = "Low" | "Medium" | "High" | "Urgent";

/** Page object for the single-page task UI. Selectors are roles and labels only. */
export class TasksPage {
  readonly sidebar: Locator;
  readonly dialog: Locator;
  readonly alert: Locator;

  constructor(readonly page: Page) {
    this.sidebar = page.getByRole("navigation", { name: "Categories" });
    this.dialog = page.getByRole("dialog");
    this.alert = page.getByRole("alert");
  }

  async goto() {
    await this.page.goto("/");
    await expect(this.page.getByRole("heading", { name: "iHelper" })).toBeVisible();
  }

  // --- list ---
  task(title: string): Locator {
    return this.page.getByRole("button", { name: title, exact: true });
  }

  /** Narrow the list to one test's data; every test uses a unique title. */
  async search(text: string) {
    await this.page.getByLabel("Search tasks").fill(text);
  }

  async show(status: "Open" | "Done" | "All") {
    await this.page.getByLabel("Show").selectOption({ label: status });
  }

  async filterPriority(priority: Priority | "Any") {
    await this.page.getByRole("combobox", { name: "Priority", exact: true }).selectOption({ label: priority });
  }

  async sortBy(sort: "Priority" | "Due date" | "Newest" | "Title") {
    await this.page.getByLabel("Sort by").selectOption({ label: sort });
  }

  /** Task titles in rendered order. */
  async titles(): Promise<string[]> {
    return this.page.locator("li").filter({ has: this.page.getByRole("checkbox") }).getByRole("button").allInnerTexts();
  }

  // --- quick add ---
  async quickAdd(title: string, options: { priority?: Priority; category?: string } = {}) {
    await this.page.getByLabel("New task title").fill(title);
    if (options.priority) await this.page.getByLabel("New task priority").selectOption({ label: options.priority });
    if (options.category) await this.page.getByLabel("New task category").selectOption({ label: options.category });
    await this.page.getByRole("button", { name: "Add", exact: true }).click();
  }

  // --- complete / reopen ---
  async complete(title: string) {
    await this.page.getByRole("checkbox", { name: `Complete ${title}` }).click(); // the row leaves the list, so check() cannot confirm state
  }

  async reopen(title: string) {
    await this.page.getByRole("checkbox", { name: `Reopen ${title}` }).click();
  }

  // --- edit dialog ---
  async openEditor(title: string) {
    await this.task(title).click();
    await expect(this.dialog).toBeVisible();
  }

  async editTask(fields: {
    title?: string;
    description?: string;
    category?: string;
    priority?: Priority;
    dueDate?: string;
  }) {
    const d = this.dialog;
    if (fields.title !== undefined) await d.getByLabel("Title").fill(fields.title);
    if (fields.description !== undefined) await d.getByLabel("Description").fill(fields.description);
    if (fields.category) await d.getByLabel("Category").selectOption({ label: fields.category });
    if (fields.priority) await d.getByLabel("Priority").selectOption({ label: fields.priority });
    if (fields.dueDate !== undefined) await d.getByLabel("Due date").fill(fields.dueDate);
    await d.getByRole("button", { name: "Save" }).click();
    await expect(d).toBeHidden();
  }

  async deleteFromEditor() {
    await this.dialog.getByRole("button", { name: "Delete" }).click();
    await expect(this.dialog).toBeHidden();
  }

  // --- categories ---
  category(name: string): Locator {
    return this.sidebar.getByRole("button", { name: new RegExp(`^${escapeRegExp(name)}(\\s+\\d+ open)?$`) });
  }

  async selectCategory(name: string) {
    await this.category(name).click();
  }

  async addCategory(name: string) {
    await this.sidebar.getByLabel("New category name").fill(name);
    await this.sidebar.getByRole("button", { name: "Add category" }).click();
    await expect(this.category(name)).toBeVisible();
  }

  async renameCategory(from: string, to: string) {
    await this.sidebar.getByRole("button", { name: `Edit category ${from}` }).click();
    await this.sidebar.getByLabel("Category name", { exact: true }).fill(to);
    await this.sidebar.getByRole("button", { name: "Save" }).click();
    await expect(this.category(to)).toBeVisible();
  }

  async deleteCategory(name: string) {
    await this.sidebar.getByRole("button", { name: `Edit category ${name}` }).click();
    await this.sidebar.getByRole("button", { name: "Delete" }).click();
    await expect(this.category(name)).toHaveCount(0);
  }
}

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
