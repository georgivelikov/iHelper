import { createApiClient } from "@ihelper/api-client";
import { authedFetch } from "../auth/session";

// Same origin in production; in dev Vite proxies /api to the backend.
// Requests carry the bearer token and refresh it transparently on a 401.
export const api = createApiClient(window.location.origin, authedFetch);
