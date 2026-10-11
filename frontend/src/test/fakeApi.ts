import { http, HttpResponse } from "msw";
import type { Category, Task } from "@ihelper/api-client";

const NOW = "2026-03-01T10:00:00Z";

export function makeTask(over: Partial<Task> & { id: string; title: string }): Task {
  return {
    description: null,
    categoryId: null,
    priority: "MEDIUM",
    status: "TODO",
    dueDate: null,
    completedAt: null,
    createdAt: NOW,
    updatedAt: NOW,
    version: 0,
    deletedAt: null,
    ...over,
  };
}

export function makeCategory(over: Partial<Category> & { id: string; name: string }): Category {
  return { color: "#0ea5e9", createdAt: NOW, updatedAt: NOW, version: 0, deletedAt: null, ...over };
}

export interface RecordedRequest {
  method: string;
  path: string;
  search: URLSearchParams;
  body?: Record<string, unknown>;
}

const problem = (status: number, title: string, extra: Record<string, unknown> = {}) =>
  HttpResponse.json({ status, title, detail: title, ...extra }, {
    status,
    headers: { "Content-Type": "application/problem+json" },
  });

/**
 * A small in-memory stand-in for the backend: same routes, same status codes, same version
 * checks. Tests inspect `requests` to see exactly what the UI sent.
 */
export function createFakeApi(seed: { categories?: Category[]; tasks?: Task[] } = {}) {
  const state = {
    categories: [...(seed.categories ?? [])],
    tasks: [...(seed.tasks ?? [])],
    requests: [] as RecordedRequest[],
    /** When set, the next PATCH to this task id answers 409 with `current`. */
    forceConflict: new Map<string, Task>(),
    /** When set, the next POST /tasks answers 400 with these field errors. */
    rejectCreate: null as null | { field: string; message: string },
  };

  const record = async (request: Request, body = false) => {
    const url = new URL(request.url);
    state.requests.push({
      method: request.method,
      path: url.pathname,
      search: url.searchParams,
      body: body ? ((await request.json()) as Record<string, unknown>) : undefined,
    });
  };

  const handlers = [
    http.get("*/api/v1/categories", async ({ request }) => {
      await record(request);
      return HttpResponse.json(state.categories);
    }),
    http.post("*/api/v1/categories", async ({ request }) => {
      await record(request.clone(), true);
      const body = (await request.json()) as { id: string; name: string; color?: string };
      const created = makeCategory({ id: body.id, name: body.name, color: body.color ?? null });
      state.categories.push(created);
      return HttpResponse.json(created, { status: 201 });
    }),
    http.put("*/api/v1/categories/:id", async ({ request, params }) => {
      await record(request.clone(), true);
      const body = (await request.json()) as { name: string; color?: string; version: number };
      const cat = state.categories.find((c) => c.id === params.id)!;
      if (cat.version !== body.version) return problem(409, "Conflict", { current: cat });
      Object.assign(cat, { name: body.name, color: body.color ?? null, version: cat.version + 1 });
      return HttpResponse.json(cat);
    }),
    http.delete("*/api/v1/categories/:id", async ({ request, params }) => {
      await record(request);
      state.categories = state.categories.filter((c) => c.id !== params.id);
      state.tasks = state.tasks.map((t) => (t.categoryId === params.id ? { ...t, categoryId: null } : t));
      return new HttpResponse(null, { status: 204 });
    }),

    http.get("*/api/v1/tasks/counts", async ({ request }) => {
      await record(request);
      const open = state.tasks.filter((t) => t.status !== "DONE");
      const byCategory: Record<string, number> = {};
      for (const t of open) if (t.categoryId) byCategory[t.categoryId] = (byCategory[t.categoryId] ?? 0) + 1;
      return HttpResponse.json({
        total: open.length,
        inbox: open.filter((t) => !t.categoryId).length,
        byCategory,
      });
    }),
    http.get("*/api/v1/tasks", async ({ request }) => {
      await record(request);
      const q = new URL(request.url).searchParams;
      const statuses = q.getAll("status");
      let items = state.tasks.filter((t) => !t.deletedAt);
      if (statuses.length) items = items.filter((t) => statuses.includes(t.status));
      if (q.get("inbox") === "true") items = items.filter((t) => !t.categoryId);
      if (q.get("categoryId")) items = items.filter((t) => t.categoryId === q.get("categoryId"));
      if (q.get("priority")) items = items.filter((t) => t.priority === q.get("priority"));
      if (q.get("q")) items = items.filter((t) => t.title.toLowerCase().includes(q.get("q")!.toLowerCase()));
      return HttpResponse.json({ items, page: 0, size: 200, total: items.length });
    }),
    http.post("*/api/v1/tasks", async ({ request }) => {
      await record(request.clone(), true);
      if (state.rejectCreate) {
        return problem(400, "Validation failed", { errors: [state.rejectCreate] });
      }
      const body = (await request.json()) as Partial<Task> & { id: string; title: string };
      const created = makeTask({ ...body });
      state.tasks.push(created);
      return HttpResponse.json(created, { status: 201 });
    }),
    http.patch("*/api/v1/tasks/:id", async ({ request, params }) => {
      await record(request.clone(), true);
      const body = (await request.json()) as Partial<Task> & { version?: number };
      const id = params.id as string;
      const forced = state.forceConflict.get(id);
      if (forced) {
        state.forceConflict.delete(id);
        state.tasks = state.tasks.map((t) => (t.id === id ? forced : t));
        return problem(409, "Conflict", { current: forced });
      }
      const task = state.tasks.find((t) => t.id === id)!;
      if (body.version !== undefined && body.version !== task.version) {
        return problem(409, "Conflict", { current: task });
      }
      const { version: _v, ...fields } = body;
      Object.assign(task, fields, { version: task.version + 1 });
      if (fields.status === "DONE") task.completedAt = NOW;
      return HttpResponse.json(task);
    }),
    http.delete("*/api/v1/tasks/:id", async ({ request, params }) => {
      await record(request);
      state.tasks = state.tasks.filter((t) => t.id !== params.id);
      return new HttpResponse(null, { status: 204 });
    }),
  ];

  return { handlers, state };
}
