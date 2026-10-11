import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { beforeEach, describe, expect, it } from "vitest";
import { App } from "../App";
import { createFakeApi, makeCategory, makeTask } from "../test/fakeApi";
import { renderWithClient } from "../test/render";
import { server } from "../test/setup";
import { AuthGate } from "./AuthGate";
import { resetSessionForTests } from "./session";

const problem = (status: number, title: string, detail = title) =>
  HttpResponse.json({ status, title, detail }, { status, headers: { "Content-Type": "application/problem+json" } });

const user = { id: "u-1", username: "georgi" };
const tokenResponse = (accessToken: string) => ({ accessToken, tokenType: "Bearer", expiresIn: 900, user });

interface Options {
  /** Whether the refresh cookie is valid when the page loads. */
  hasCookie?: boolean;
  tasks?: ReturnType<typeof makeTask>[];
}

function setup({ hasCookie = false, tasks = [makeTask({ id: "t1", title: "Ship release" })] }: Options = {}) {
  const calls = { login: [] as unknown[], register: [] as unknown[], refresh: 0, logout: 0, taskAuth: [] as (string | null)[] };
  const state = { cookie: hasCookie, validToken: "token-1", nextToken: "token-2" };
  const api = createFakeApi({ categories: [makeCategory({ id: "c1", name: "Work" })], tasks });

  server.use(
    http.post("*/api/v1/auth/login", async ({ request }) => {
      const body = (await request.json()) as { username: string; password: string };
      calls.login.push(body);
      if (body.password !== "correct-horse-battery") return problem(401, "Unauthorized", "Invalid username or password");
      state.cookie = true;
      return HttpResponse.json(tokenResponse(state.validToken));
    }),
    http.post("*/api/v1/auth/register", async ({ request }) => {
      const body = (await request.json()) as { username: string; password: string; inviteCode: string | null };
      calls.register.push(body);
      if (body.inviteCode !== "welcome") return problem(403, "Forbidden", "A valid invite code is required to register");
      state.cookie = true;
      return HttpResponse.json(tokenResponse(state.validToken), { status: 201 });
    }),
    http.post("*/api/v1/auth/refresh", () => {
      calls.refresh += 1;
      if (!state.cookie) return problem(401, "Unauthorized", "Invalid or expired refresh token");
      state.validToken = state.nextToken;
      return HttpResponse.json(tokenResponse(state.validToken));
    }),
    http.post("*/api/v1/auth/logout", () => {
      calls.logout += 1;
      state.cookie = false;
      return new HttpResponse(null, { status: 204 });
    }),
    // Tasks answer 401 unless the request carries the currently valid token.
    http.get("*/api/v1/tasks", ({ request }) => {
      calls.taskAuth.push(request.headers.get("Authorization"));
      if (request.headers.get("Authorization") !== `Bearer ${state.validToken}`) return problem(401, "Unauthorized");
      return undefined; // fall through to the fake API handler
    }),
    ...api.handlers,
  );

  renderWithClient(<AuthGate>{(u) => <App user={u} />}</AuthGate>);
  return { calls, state, api };
}

describe("AuthGate", () => {
  beforeEach(() => resetSessionForTests());

  it("shows the sign-in form when there is no session", async () => {
    setup();

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(screen.queryByText("Ship release")).not.toBeInTheDocument();
  });

  it("restores the session from the refresh cookie without asking for a password", async () => {
    const { calls } = setup({ hasCookie: true });

    expect(await screen.findByText("Ship release")).toBeInTheDocument();
    expect(screen.getByText("georgi")).toBeInTheDocument();
    expect(calls.refresh).toBe(1);
    expect(calls.login).toHaveLength(0);
  });

  it("signs in and sends the bearer token with API calls", async () => {
    const { calls } = setup();
    await screen.findByRole("heading", { name: "Sign in" });

    await userEvent.type(screen.getByLabelText("Username"), "georgi");
    await userEvent.type(screen.getByLabelText("Password"), "correct-horse-battery");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("Ship release")).toBeInTheDocument();
    expect(calls.login).toEqual([{ username: "georgi", password: "correct-horse-battery" }]);
    expect(calls.taskAuth.at(-1)).toBe("Bearer token-1");
  });

  it("shows the server message for a wrong password and stays on the form", async () => {
    setup();
    await screen.findByRole("heading", { name: "Sign in" });

    await userEvent.type(screen.getByLabelText("Username"), "georgi");
    await userEvent.type(screen.getByLabelText("Password"), "not-the-password");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid username or password");
    expect(screen.getByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });

  it("registers with an invite code", async () => {
    const { calls } = setup();
    await userEvent.click(await screen.findByRole("button", { name: "Create an account" }));

    await userEvent.type(screen.getByLabelText("Username"), "newuser");
    await userEvent.type(screen.getByLabelText("Password"), "correct-horse-battery");
    await userEvent.type(screen.getByLabelText("Invite code"), "welcome");
    await userEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByText("Ship release")).toBeInTheDocument();
    expect(calls.register).toEqual([
      { username: "newuser", password: "correct-horse-battery", inviteCode: "welcome" },
    ]);
  });

  it("reports a rejected invite code", async () => {
    setup();
    await userEvent.click(await screen.findByRole("button", { name: "Create an account" }));

    await userEvent.type(screen.getByLabelText("Username"), "newuser");
    await userEvent.type(screen.getByLabelText("Password"), "correct-horse-battery");
    await userEvent.type(screen.getByLabelText("Invite code"), "wrong");
    await userEvent.click(screen.getByRole("button", { name: "Create account" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("A valid invite code is required");
  });

  it("refreshes the access token once and retries when an API call answers 401", async () => {
    const { calls, state } = setup({ hasCookie: true });
    await screen.findByText("Ship release");
    const refreshesAfterLoad = calls.refresh;

    // The access token expires server-side; the next list request must recover on its own.
    state.validToken = "token-expired-then-rotated";
    state.nextToken = "token-3";
    await userEvent.selectOptions(screen.getByLabelText("Show"), "open");

    await waitFor(() => expect(calls.refresh).toBe(refreshesAfterLoad + 1));
    await waitFor(() => expect(calls.taskAuth.at(-1)).toBe("Bearer token-3"));
    expect(screen.getByText("Ship release")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Sign in" })).not.toBeInTheDocument();
  });

  it("falls back to the sign-in form when the session cannot be refreshed", async () => {
    const { state } = setup({ hasCookie: true });
    await screen.findByText("Ship release");

    state.validToken = "something-else";
    state.cookie = false; // refresh cookie expired or revoked
    await userEvent.selectOptions(screen.getByLabelText("Show"), "done");

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
  });

  it("signs out: revokes the cookie and returns to the sign-in form", async () => {
    const { calls } = setup({ hasCookie: true });
    await screen.findByText("Ship release");

    await userEvent.click(screen.getByRole("button", { name: "Sign out" }));

    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeInTheDocument();
    expect(calls.logout).toBe(1);
    expect(screen.queryByText("Ship release")).not.toBeInTheDocument();
  });
});
