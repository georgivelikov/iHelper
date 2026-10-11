import { useState } from "react";
import type { Priority, Task, User } from "@ihelper/api-client";
import { signOut } from "./auth/session";
import {
  useBin,
  useCategories,
  useCreateCategory,
  useCreateTask,
  useDeleteCategory,
  useDeleteTask,
  useEmptyBin,
  usePatchTask,
  useReorderCategories,
  useRestoreTask,
  useTaskCounts,
  useTasks,
  useUpdateCategory,
  PAGE_SIZE,
  type SortKey,
  type StatusFilter,
} from "./api/hooks";
import { BinView } from "./components/BinView";
import { CategorySidebar } from "./components/CategorySidebar";
import { ErrorBanner } from "./components/ErrorBanner";
import { Filters } from "./components/Filters";
import { QuickAdd } from "./components/QuickAdd";
import { TaskEditDialog } from "./components/TaskEditDialog";
import { TaskList } from "./components/TaskList";
import styles from "./App.module.css";

export function App({ user }: { user?: User }) {
  const [view, setView] = useState("all");
  // Finished tasks stay visible (and sink to the bottom) until the user filters them away.
  const [status, setStatus] = useState<StatusFilter>("all");
  const [priority, setPriority] = useState<Priority | "">("");
  const [sort, setSort] = useState<SortKey>("priority");
  const [q, setQ] = useState("");
  const [editing, setEditing] = useState<Task | null>(null);

  const categoriesQuery = useCategories();
  const counts = useTaskCounts();
  const tasksQuery = useTasks({ view, status, priority, sort, q });
  const inBin = view === "bin";
  const binQuery = useBin(inBin);

  const createTask = useCreateTask();
  const patchTask = usePatchTask();
  const deleteTask = useDeleteTask();
  const createCategory = useCreateCategory();
  const updateCategory = useUpdateCategory();
  const deleteCategory = useDeleteCategory();
  const reorderCategories = useReorderCategories();
  const restoreTask = useRestoreTask();
  const emptyBin = useEmptyBin();

  const categories = categoriesQuery.data ?? [];
  const tasks = tasksQuery.data?.items ?? [];

  // Errors from the toolbar/list actions; the edit dialog shows its own save error.
  const bannerError =
    tasksQuery.error ??
    binQuery.error ??
    categoriesQuery.error ??
    createTask.error ??
    deleteTask.error ??
    createCategory.error ??
    updateCategory.error ??
    deleteCategory.error ??
    reorderCategories.error ??
    restoreTask.error ??
    emptyBin.error ??
    (editing ? null : patchTask.error);

  function dismissErrors() {
    [
      createTask,
      deleteTask,
      createCategory,
      updateCategory,
      deleteCategory,
      reorderCategories,
      restoreTask,
      emptyBin,
      patchTask,
    ].forEach((m) => m.reset());
  }

  function toggle(task: Task) {
    patchTask.mutate({
      id: task.id,
      version: task.version,
      fields: { status: task.status === "DONE" ? "TODO" : "DONE" },
    });
  }

  function closeDialog() {
    setEditing(null);
    patchTask.reset();
  }

  const defaultCategory = view === "all" || view === "inbox" || view === "bin" ? "" : view;

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <h1>iHelper</h1>
        {user && (
          <div className={styles.account}>
            <span>{user.username}</span>
            <button type="button" onClick={() => void signOut()}>
              Sign out
            </button>
          </div>
        )}
      </header>
      <aside className={styles.aside}>
        <CategorySidebar
          categories={categories}
          counts={counts.data}
          selected={view}
          onSelect={setView}
          onCreate={(c) => createCategory.mutate(c)}
          onUpdate={(c) => updateCategory.mutate(c)}
          onDelete={(c) => {
            if (c.id === view) setView("all");
            deleteCategory.mutate(c);
          }}
          onReorder={(ids) => reorderCategories.mutate(ids)}
        />
      </aside>
      <main className={styles.main}>
        <ErrorBanner error={bannerError} onDismiss={dismissErrors} />
        {inBin ? (
          binQuery.isLoading ? (
            <p role="status">Loading…</p>
          ) : (
            <BinView
              tasks={binQuery.data?.items ?? []}
              categories={categories}
              busy={restoreTask.isPending || emptyBin.isPending}
              onRestore={(t) => restoreTask.mutate({ id: t.id, version: t.version })}
              onEmpty={() => emptyBin.mutate()}
            />
          )
        ) : (
          <>
            <QuickAdd
              key={defaultCategory}
              categories={categories}
              defaultCategoryId={defaultCategory}
              disabled={createTask.isPending}
              onAdd={(t) => createTask.mutate(t)}
            />
            <Filters
              status={status}
              priority={priority}
              sort={sort}
              q={q}
              onStatus={setStatus}
              onPriority={setPriority}
              onSort={setSort}
              onSearch={setQ}
            />
            {tasksQuery.isLoading ? (
              <p role="status">Loading…</p>
            ) : (
              <TaskList
                tasks={tasks}
                categories={categories}
                sort={sort}
                onToggle={toggle}
                onEdit={setEditing}
                onDelete={(t) => deleteTask.mutate({ id: t.id, version: t.version })}
              />
            )}
            {tasksQuery.data && tasksQuery.data.total > PAGE_SIZE && (
              <p className={styles.note}>
                Showing the first {PAGE_SIZE} of {tasksQuery.data.total} tasks. Narrow the filters to see the rest.
              </p>
            )}
          </>
        )}
      </main>
      {editing && (
        <TaskEditDialog
          task={editing}
          categories={categories}
          saving={patchTask.isPending}
          error={patchTask.error}
          onClose={closeDialog}
          onSave={(input) =>
            patchTask.mutate({ id: editing.id, ...input }, { onSuccess: closeDialog })
          }
          onDelete={(version) =>
            deleteTask.mutate({ id: editing.id, version }, { onSuccess: closeDialog })
          }
        />
      )}
    </div>
  );
}
