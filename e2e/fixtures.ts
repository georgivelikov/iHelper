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

export const PASSWORD = "correct-horse-battery";

export interface Account {
  username: string;
  userId: string;
  accessToken: string;
}

export interface Api {
  uniq(label: string): string;
  createTask(seed: TaskSeed): Promise<{ id: string; title: string }>;
  createCategory(name: string): Promise<{ id: string; name: string }>;
  listTasks(q: string): Promise<{ id: string; title: string; status: string; categoryId: string | null }[]>;
  listCategories(): Promise<{ id: string; name: string }[]>;
}

function makeApi(request: APIRequestContext, accessToken: string): Api {
  const auth = { Authorization: `Bearer ${accessToken}` };
  return {
    // Unique per call, so parallel tests (and both viewports) never see each other's data.
    uniq: (label) => `${label} ${randomUUID().slice(0, 8)}`,

    async createTask(seed) {
      const id = randomUUID();
      const res = await request.post("/api/v1/tasks", { headers: auth, data: { id, priority: "MEDIUM", ...seed } });
      expect(res.status()).toBe(201);
      return { id, title: seed.title };
    },

    async createCategory(name) {
      const id = randomUUID();
      const res = await request.post("/api/v1/categories", { headers: auth, data: { id, name, color: "#3b82f6" } });
      expect(res.status()).toBe(201);
      return { id, name };
    },

    async listTasks(q) {
      const params = new URLSearchParams({ q });
      for (const s of ["TODO", "IN_PROGRESS", "DONE"]) params.append("status", s);
      const res = await request.get(`/api/v1/tasks?${params}`, { headers: auth });
      expect(res.ok()).toBeTruthy();
      return (await res.json()).items;
    },

    async listCategories() {
      const res = await request.get("/api/v1/categories", { headers: auth });
      expect(res.ok()).toBeTruthy();
      return res.json();
    },
  };
}

export const test = base.extend<{ account: Account; api: Api; tasks: TasksPage }>({
  // Every test gets its own brand-new user, so tests are isolated and parallel-safe by construction.
  // The browser is signed in by handing it the refresh cookie; the page restores the session on load.
  account: async ({ request, context }, use) => {
    const username = `e2e${randomUUID().replace(/-/g, "").slice(0, 16)}`;
    const res = await request.post("/api/v1/auth/register", {
      data: { username, password: PASSWORD, inviteCode: process.env.E2E_INVITE_CODE },
    });
    expect(res.status()).toBe(201);
    const body = await res.json();
    await context.addCookies((await request.storageState()).cookies);
    await use({ username, userId: body.user.id, accessToken: body.accessToken });
  },
  api: async ({ request, account }, use) => use(makeApi(request, account.accessToken)),
  // Tests seed data through `api` first, then call `tasks.goto()`.
  tasks: async ({ page, account }, use) => {
    void account; // signs the browser in before the page object is used
    await use(new TasksPage(page));
  },
});

export { expect };
