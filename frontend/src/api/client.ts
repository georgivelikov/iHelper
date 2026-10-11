import { createApiClient } from "@ihelper/api-client";

// Same origin in production; in dev Vite proxies /api to the backend.
export const api = createApiClient(window.location.origin);
