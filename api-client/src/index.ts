import createClient from "openapi-fetch";
import type { components, paths } from "./schema";

export type { components, paths } from "./schema";

export type Category = components["schemas"]["CategoryDto"];
export type Task = components["schemas"]["TaskDto"];
export type TaskCounts = components["schemas"]["TaskCounts"];
export type SyncResponse = components["schemas"]["SyncResponse"];
export type AuthResponse = components["schemas"]["AuthResponse"];
export type User = components["schemas"]["UserDto"];
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

/**
 * Typed client for the iHelper API. `baseUrl` is "" in the web app (same origin / dev proxy).
 * Pass `fetchImpl` to wrap every request, e.g. to attach a bearer token and refresh it on 401.
 */
export function createApiClient(baseUrl = "", fetchImpl?: (request: Request) => Promise<Response>) {
  // Resolve fetch per call (not once at creation) so test doubles and polyfills installed later apply.
  return createClient<paths>({ baseUrl, fetch: fetchImpl ?? ((request) => globalThis.fetch(request)) });
}

export type ApiClient = ReturnType<typeof createApiClient>;
