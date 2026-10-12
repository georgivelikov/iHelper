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
    return this.page
      .locator("li")
      .filter({ has: this.page.getByRole("checkbox") })
      .locator('button:not([aria-label^="Delete "]):not([aria-label^="Edit "])')
      .allInnerTexts();
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
    await this.page.getByRole("checkbox", { name: `Complete ${title}` }).click();
  }

  async reopen(title: string) {
    await this.page.getByRole("checkbox", { name: `Reopen ${title}` }).click();
  }

  /** The delete icon on a row: moves the task to the bin. */
  async deleteTask(title: string) {
    await this.page.getByRole("button", { name: `Delete ${title}`, exact: true }).click();
  }

  // --- bin ---
  binEntry(): Locator {
    return this.sidebar.getByRole("button", { name: /^Bin(\s+\d+ deleted)?$/ });
  }

  async openBin() {
    await this.binEntry().click();
    await expect(this.page.getByRole("region", { name: "Bin" })).toBeVisible();
  }

  /** A task as listed inside the bin. */
  binTask(title: string): Locator {
    return this.page.getByRole("region", { name: "Bin" }).getByText(title, { exact: true });
  }

  async restoreFromBin(title: string) {
    await this.page.getByRole("button", { name: `Restore ${title}`, exact: true }).click();
  }

  /** Ticks tasks in the bin, then erases just those (asks for confirmation first). */
  async deleteSelectedFromBin(...titles: string[]) {
    for (const title of titles) {
      await this.page.getByRole("checkbox", { name: `Select ${title}`, exact: true }).check();
    }
    await this.page.getByRole("button", { name: /^Delete selected/ }).click();
    await this.page.getByRole("button", { name: "Erase forever", exact: true }).click();
  }

  /** Erases everything in the bin: Select all, then Delete selected. */
  async eraseWholeBin() {
    await this.page.getByRole("checkbox", { name: "Select all", exact: true }).check();
    await this.page.getByRole("button", { name: /^Delete selected/ }).click();
    await this.page.getByRole("button", { name: "Erase forever", exact: true }).click();
  }

  // --- edit dialog ---
  /** The edit icon on a row opens the editor. (Clicking the row itself only expands it.) */
  async openEditor(title: string) {
    await this.page.getByRole("button", { name: `Edit ${title}`, exact: true }).click();
    await expect(this.dialog).toBeVisible();
  }

  async editTask(fields: {
    title?: string;
    notes?: string;
    category?: string;
    priority?: Priority;
    dueDate?: string;
  }) {
    const d = this.dialog;
    if (fields.title !== undefined) await d.getByLabel("Title").fill(fields.title);
    if (fields.notes !== undefined) await d.getByLabel("Notes").fill(fields.notes);
    if (fields.category) await d.getByLabel("Category").selectOption({ label: fields.category });
    if (fields.priority) await d.getByLabel("Priority").selectOption({ label: fields.priority });
    if (fields.dueDate !== undefined) await d.getByLabel("Due date").fill(fields.dueDate);
    await d.getByRole("button", { name: "Save" }).click();
    await expect(d).toBeHidden();
  }

  async deleteFromEditor() {
    await this.dialog.getByRole("button", { name: "Delete", exact: true }).click();
    await expect(this.dialog).toBeHidden();
  }

  // --- categories ---
  category(name: string): Locator {
    return this.sidebar.getByRole("button", { name: new RegExp(`^${escapeRegExp(name)}(\\s+\\d+ (open|deleted))?$`) });
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

  /** Names of the user's categories, top (or left) first. */
  async categoryOrder(): Promise<string[]> {
    const texts = await this.sidebar
      .getByRole("list", { name: "Your categories" })
      .getByRole("button", { name: / \d+ open$/ })
      .allInnerTexts();
    return texts.map((t) => t.split("\n")[0].trim());
  }

  /** Reorders with the keyboard: Space picks the handle up, arrows move it, Space drops it. */
  async moveCategory(name: string, steps: number) {
    const horizontal = (this.page.viewportSize()?.width ?? 1280) <= 720; // the sidebar becomes a row on phones
    const forward = horizontal ? "ArrowRight" : "ArrowDown";
    const back = horizontal ? "ArrowLeft" : "ArrowUp";
    const handle = this.sidebar.getByRole("button", { name: `Reorder category ${name}` });
    await handle.focus();
    await this.page.keyboard.press("Space");
    await expect(handle).toHaveAttribute("aria-pressed", "true"); // picked up; arrows before this are lost
    for (let i = 0; i < Math.abs(steps); i++) {
      await this.page.keyboard.press(steps > 0 ? forward : back);
      await this.page.waitForTimeout(100); // let the sensor settle on the new slot before the next key
    }
    await this.page.keyboard.press("Space");
    await expect(handle).not.toHaveAttribute("aria-pressed", "true");
  }

  /** Reorders with a real pointer drag of the handle onto another category. */
  async dragCategory(name: string, onto: string) {
    const handle = await this.sidebar.getByRole("button", { name: `Reorder category ${name}` }).boundingBox();
    const target = await this.category(onto).boundingBox();
    if (!handle || !target) throw new Error("category not visible");
    const from = { x: handle.x + handle.width / 2, y: handle.y + handle.height / 2 };
    const to = { x: target.x + target.width / 2, y: target.y + target.height / 2 };
    await this.page.mouse.move(from.x, from.y);
    await this.page.mouse.down();
    await this.page.mouse.move(from.x + 6, from.y + 6, { steps: 3 });
    await this.page.mouse.move(to.x, to.y, { steps: 12 });
    await this.page.mouse.up();
  }

  async deleteCategory(name: string) {
    await this.sidebar.getByRole("button", { name: `Edit category ${name}` }).click();
    await this.sidebar.getByRole("button", { name: "Delete", exact: true }).click();
    await expect(this.category(name)).toHaveCount(0);
  }
}

function escapeRegExp(s: string) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
