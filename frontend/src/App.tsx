import { useState } from "react";
import type { Priority, Task } from "@ihelper/api-client";
import {
  useCategories,
  useCreateCategory,
  useCreateTask,
  useDeleteCategory,
  useDeleteTask,
  usePatchTask,
  useTaskCounts,
  useTasks,
  useUpdateCategory,
  PAGE_SIZE,
  type SortKey,
  type StatusFilter,
} from "./api/hooks";
import { CategorySidebar } from "./components/CategorySidebar";
import { ErrorBanner } from "./components/ErrorBanner";
import { Filters } from "./components/Filters";
import { QuickAdd } from "./components/QuickAdd";
import { TaskEditDialog } from "./components/TaskEditDialog";
import { TaskList } from "./components/TaskList";
import styles from "./App.module.css";

export function App() {
  const [view, setView] = useState("all");
  const [status, setStatus] = useState<StatusFilter>("open");
  const [priority, setPriority] = useState<Priority | "">("");
  const [sort, setSort] = useState<SortKey>("priority");
  const [q, setQ] = useState("");
  const [editing, setEditing] = useState<Task | null>(null);

  const categoriesQuery = useCategories();
  const counts = useTaskCounts();
  const tasksQuery = useTasks({ view, status, priority, sort, q });

  const createTask = useCreateTask();
  const patchTask = usePatchTask();
  const deleteTask = useDeleteTask();
  const createCategory = useCreateCategory();
  const updateCategory = useUpdateCategory();
  const deleteCategory = useDeleteCategory();

  const categories = categoriesQuery.data ?? [];
  const tasks = tasksQuery.data?.items ?? [];

  // Errors from the toolbar/list actions; the edit dialog shows its own save error.
  const bannerError =
    tasksQuery.error ??
    categoriesQuery.error ??
    createTask.error ??
    deleteTask.error ??
    createCategory.error ??
    updateCategory.error ??
    deleteCategory.error ??
    (editing ? null : patchTask.error);

  function dismissErrors() {
    [createTask, deleteTask, createCategory, updateCategory, deleteCategory, patchTask].forEach((m) => m.reset());
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

  const defaultCategory = view === "all" || view === "inbox" ? "" : view;

  return (
    <div className={styles.layout}>
      <header className={styles.header}>
        <h1>iHelper</h1>
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
        />
      </aside>
      <main className={styles.main}>
        <ErrorBanner error={bannerError} onDismiss={dismissErrors} />
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
          <TaskList tasks={tasks} categories={categories} sort={sort} onToggle={toggle} onEdit={setEditing} />
        )}
        {tasksQuery.data && tasksQuery.data.total > PAGE_SIZE && (
          <p className={styles.note}>
            Showing the first {PAGE_SIZE} of {tasksQuery.data.total} tasks. Narrow the filters to see the rest.
          </p>
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
