import { randomUUID } from "node:crypto";
import { test as base, expect, type APIRequestContext } from "@playwright/test";
import { TasksPage } from "./pages/TasksPage";

export interface TaskSeed {
  title: string;
  priority?: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  status?: "TODO" | "IN_PROGRESS" | "DONE";
  categoryId?: string;
  dueDate?: string;
  description?: string;
}

export interface Api {
  uniq(label: string): string;
  createTask(seed: TaskSeed): Promise<{ id: string; title: string }>;
  createCategory(name: string): Promise<{ id: string; name: string }>;
  listTasks(q: string): Promise<{ id: string; title: string; status: string; categoryId: string | null }[]>;
  listCategories(): Promise<{ id: string; name: string }[]>;
}

function makeApi(request: APIRequestContext): Api {
  return {
    // Unique per call, so parallel tests (and both viewports) never see each other's data.
    uniq: (label) => `${label} ${randomUUID().slice(0, 8)}`,

    async createTask(seed) {
      const id = randomUUID();
      const res = await request.post("/api/v1/tasks", { data: { id, priority: "MEDIUM", ...seed } });
      expect(res.status()).toBe(201);
      return { id, title: seed.title };
    },

    async createCategory(name) {
      const id = randomUUID();
      const res = await request.post("/api/v1/categories", { data: { id, name, color: "#3b82f6" } });
      expect(res.status()).toBe(201);
      return { id, name };
    },

    async listTasks(q) {
      const params = new URLSearchParams({ q });
      for (const s of ["TODO", "IN_PROGRESS", "DONE"]) params.append("status", s);
      const res = await request.get(`/api/v1/tasks?${params}`);
      expect(res.ok()).toBeTruthy();
      return (await res.json()).items;
    },

    async listCategories() {
      const res = await request.get("/api/v1/categories");
      expect(res.ok()).toBeTruthy();
      return res.json();
    },
  };
}

export const test = base.extend<{ api: Api; tasks: TasksPage }>({
  api: async ({ request }, use) => use(makeApi(request)),
  // Tests seed data through `api` first, then call `tasks.goto()`.
  tasks: async ({ page }, use) => use(new TasksPage(page)),
});

export { expect };
