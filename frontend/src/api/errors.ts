import type { ProblemDetails, Task, Category } from "@ihelper/api-client";

/** A non-2xx API answer. `problem` is the RFC 9457 body when the server sent one. */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetails;

  constructor(status: number, problem: ProblemDetails | undefined) {
    super(problem?.detail ?? problem?.title ?? `Request failed (${status})`);
    this.name = "ApiError";
    this.status = status;
    this.problem = problem ?? {};
  }

  get isConflict(): boolean {
    return this.status === 409;
  }

  /** The server's current copy, present on version conflicts. */
  get current(): Task | Category | undefined {
    return this.problem.current;
  }
}

interface Result<T> {
  data?: T;
  error?: unknown;
  response: Response;
}

/** Returns `data`, or throws ApiError (also for network-less/empty bodies on error statuses). */
export function unwrap<T>(result: Result<T>): T {
  if (result.error !== undefined || !result.response.ok) {
    throw new ApiError(result.response.status, result.error as ProblemDetails | undefined);
  }
  return result.data as T;
}
