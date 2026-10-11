import createClient from "openapi-fetch";
import type { components, paths } from "./schema";

export type { components, paths } from "./schema";

export type Category = components["schemas"]["CategoryDto"];
export type Task = components["schemas"]["TaskDto"];
export type TaskCounts = components["schemas"]["TaskCounts"];
export type SyncResponse = components["schemas"]["SyncResponse"];
export type Priority = Task["priority"];
export type TaskStatus = Task["status"];

/** RFC 9457 body returned by every error response; 409s also carry the server's `current` copy. */
export interface ProblemDetails {
  title?: string;
  status?: number;
  detail?: string;
  errors?: { field: string; message: string }[];
  current?: Task | Category;
}

/** Typed client for the iHelper API. `baseUrl` is "" in the web app (same origin / dev proxy). */
export function createApiClient(baseUrl = "") {
  // Resolve fetch per call (not once at creation) so test doubles and polyfills installed later apply.
  return createClient<paths>({ baseUrl, fetch: (request) => globalThis.fetch(request) });
}

export type ApiClient = ReturnType<typeof createApiClient>;
