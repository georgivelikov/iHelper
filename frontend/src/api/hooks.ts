import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { Category, Priority, TaskStatus } from "@ihelper/api-client";
import { api } from "./client";
import { unwrap } from "./errors";

/** "all" (Home), "inbox", "bin", or a category id. */
export type View = string;
export type StatusFilter = "open" | "done" | "all";
export type SortKey = "priority" | "due" | "newest" | "title";

export interface TaskQuery {
  view: View;
  status: StatusFilter;
  priority: Priority | "";
  sort: SortKey;
  q: string;
}

const SORT_PARAM: Record<SortKey, string | undefined> = {
  priority: undefined, // server default: -priority,dueDate,createdAt
  due: "dueDate,-priority",
  newest: "-createdAt",
  title: "title",
};

const STATUS_PARAM: Record<StatusFilter, TaskStatus[] | undefined> = {
  open: ["TODO", "IN_PROGRESS"],
  done: ["DONE"],
  all: undefined,
};

export const PAGE_SIZE = 200;

export function useCategories() {
  return useQuery({
    queryKey: ["categories"],
    queryFn: async () => unwrap(await api.GET("/api/v1/categories")),
  });
}

export function useTaskCounts() {
  return useQuery({
    queryKey: ["counts"],
    queryFn: async () => unwrap(await api.GET("/api/v1/tasks/counts")),
  });
}

export function useTasks(query: TaskQuery) {
  return useQuery({
    queryKey: ["tasks", query],
    enabled: query.view !== "bin",
    queryFn: async () =>
      unwrap(
        await api.GET("/api/v1/tasks", {
          params: {
            query: {
              inbox: query.view === "inbox" ? true : undefined,
              categoryId: query.view !== "all" && query.view !== "inbox" ? query.view : undefined,
              status: STATUS_PARAM[query.status],
              priority: query.priority || undefined,
              q: query.q.trim() || undefined,
              sort: SORT_PARAM[query.sort],
              size: PAGE_SIZE,
            },
          },
        }),
      ),
  });
}

/** The bin: deleted tasks waiting to be restored or erased for good. */
export function useBin(enabled: boolean) {
  return useQuery({
    queryKey: ["bin"],
    enabled,
    queryFn: async () => unwrap(await api.GET("/api/v1/tasks/bin", { params: { query: { size: PAGE_SIZE } } })),
  });
}

/** Every mutation refreshes whatever the change might have touched. */
function useInvalidate() {
  const client = useQueryClient();
  return () =>
    Promise.all([
      client.invalidateQueries({ queryKey: ["tasks"] }),
      client.invalidateQueries({ queryKey: ["bin"] }),
      client.invalidateQueries({ queryKey: ["counts"] }),
      client.invalidateQueries({ queryKey: ["categories"] }),
    ]);
}

export interface NewTask {
  title: string;
  priority: Priority;
  categoryId?: string;
}

export function useCreateTask() {
  const invalidate = useInvalidate();
  return useMutation({
    // The client picks the id, so a retried request can never create a duplicate.
    mutationFn: async (task: NewTask) =>
      unwrap(await api.POST("/api/v1/tasks", { body: { id: crypto.randomUUID(), ...task } })),
    onSuccess: invalidate,
  });
}

export interface TaskPatchInput {
  id: string;
  version: number;
  fields: {
    title?: string;
    description?: string | null;
    categoryId?: string | null;
    priority?: Priority;
    status?: TaskStatus;
    dueDate?: string | null;
  };
}

export function usePatchTask() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async ({ id, version, fields }: TaskPatchInput) =>
      unwrap(
        await api.PATCH("/api/v1/tasks/{id}", {
          params: { path: { id } },
          // null clears a field; the generated type only models "absent", hence the cast
          body: { ...fields, version } as never,
        }),
      ),
    // a conflict must also refresh the list so the user sees the newer server state
    onSettled: invalidate,
  });
}

export function useDeleteTask() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async ({ id, version }: { id: string; version: number }) =>
      unwrap(
        await api.DELETE("/api/v1/tasks/{id}", {
          params: { path: { id }, header: { "If-Match": `"${version}"` } },
        }),
      ),
    onSettled: invalidate,
  });
}

export function useRestoreTask() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async ({ id, version }: { id: string; version: number }) =>
      unwrap(
        await api.POST("/api/v1/tasks/{id}/restore", {
          params: { path: { id }, header: { "If-Match": `"${version}"` } },
        }),
      ),
    onSettled: invalidate,
  });
}

/** Final deletion of only the chosen tasks in the bin. */
export function usePurgeTasks() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async (ids: string[]) => unwrap(await api.POST("/api/v1/tasks/bin/purge", { body: { ids } })),
    onSettled: invalidate,
  });
}

export function useCreateCategory() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async (body: { name: string; color?: string }) =>
      unwrap(await api.POST("/api/v1/categories", { body: { id: crypto.randomUUID(), ...body } })),
    onSuccess: invalidate,
  });
}

export function useUpdateCategory() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async (v: { id: string; version: number; name: string; color?: string }) =>
      unwrap(
        await api.PUT("/api/v1/categories/{id}", {
          params: { path: { id: v.id } },
          body: { name: v.name, color: v.color, version: v.version },
        }),
      ),
    onSettled: invalidate,
  });
}

export function useDeleteCategory() {
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async ({ id, version }: { id: string; version: number }) =>
      unwrap(
        await api.DELETE("/api/v1/categories/{id}", {
          params: { path: { id }, header: { "If-Match": `"${version}"` } },
        }),
      ),
    onSettled: invalidate,
  });
}

/** Saves a new category order. The list reorders at once and rolls back if the server refuses. */
export function useReorderCategories() {
  const client = useQueryClient();
  const invalidate = useInvalidate();
  return useMutation({
    mutationFn: async (ids: string[]) => unwrap(await api.PUT("/api/v1/categories/order", { body: { ids } })),
    onMutate: async (ids) => {
      await client.cancelQueries({ queryKey: ["categories"] });
      const previous = client.getQueryData<Category[]>(["categories"]);
      if (previous) {
        const byId = new Map(previous.map((c) => [c.id, c]));
        const listed = ids.flatMap((id) => byId.get(id) ?? []);
        client.setQueryData<Category[]>(["categories"], [...listed, ...previous.filter((c) => !ids.includes(c.id))]);
      }
      return { previous };
    },
    onError: (_error, _ids, context) => {
      if (context?.previous) client.setQueryData(["categories"], context.previous);
    },
    onSettled: invalidate,
  });
}
