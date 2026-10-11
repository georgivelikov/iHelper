import { createApiClient, type AuthResponse, type User } from "@ihelper/api-client";
import { unwrap } from "../api/errors";

export type SessionState =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "authenticated"; user: User };

// Auth endpoints go through a plain client: they must not trigger the refresh-and-retry below.
const authApi = createApiClient(window.location.origin);

const AUTH_PATH = "/api/v1/auth/";

let state: SessionState = { status: "loading" };
// The access token lives in memory only. The refresh token is an HttpOnly cookie the page cannot read.
let accessToken: string | null = null;
let refreshing: Promise<string> | null = null;
const listeners = new Set<() => void>();

function set(next: SessionState, token: string | null) {
  state = next;
  accessToken = token;
  listeners.forEach((l) => l());
}

function apply(response: AuthResponse) {
  set({ status: "authenticated", user: response.user }, response.accessToken);
}

export function getSession(): SessionState {
  return state;
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export async function signIn(username: string, password: string): Promise<void> {
  apply(unwrap(await authApi.POST("/api/v1/auth/login", { body: { username, password } })));
}

export async function register(username: string, password: string, inviteCode: string): Promise<void> {
  apply(
    unwrap(
      await authApi.POST("/api/v1/auth/register", {
        body: { username, password, inviteCode: inviteCode.trim() || null },
      }),
    ),
  );
}

export async function signOut(): Promise<void> {
  try {
    await authApi.POST("/api/v1/auth/logout", { body: {} });
  } finally {
    // Whatever the server said, this browser is signed out.
    set({ status: "anonymous" }, null);
  }
}

/** Called once at start-up: the cookie, if any, turns back into a session. */
export async function restoreSession(): Promise<void> {
  try {
    await refreshAccessToken();
  } catch {
    // refreshAccessToken already moved the session to anonymous
  }
}

/** Single flight: concurrent 401s share one refresh, so the rotating token is used once. */
export function refreshAccessToken(): Promise<string> {
  refreshing ??= (async () => {
    try {
      const response = unwrap(await authApi.POST("/api/v1/auth/refresh", { body: {} }));
      apply(response);
      return response.accessToken;
    } catch (error) {
      set({ status: "anonymous" }, null);
      throw error;
    } finally {
      refreshing = null;
    }
  })();
  return refreshing;
}

/** fetch for the API client: adds the bearer token and, on a 401, refreshes once and retries. */
export async function authedFetch(request: Request): Promise<Response> {
  if (new URL(request.url).pathname.startsWith(AUTH_PATH)) {
    return globalThis.fetch(request);
  }
  const retry = request.clone(); // the original body is consumed by the first attempt
  if (accessToken) request.headers.set("Authorization", `Bearer ${accessToken}`);
  const response = await globalThis.fetch(request);
  if (response.status !== 401) return response;

  let token: string;
  try {
    token = await refreshAccessToken();
  } catch {
    return response; // session is gone; the UI falls back to the sign-in screen
  }
  retry.headers.set("Authorization", `Bearer ${token}`);
  return globalThis.fetch(retry);
}

/** Test helper: forget everything. */
export function resetSessionForTests() {
  refreshing = null;
  set({ status: "loading" }, null);
}
