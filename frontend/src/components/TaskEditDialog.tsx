import { useState, type FormEvent } from "react";
import type { Category, Priority, Task, TaskStatus } from "@ihelper/api-client";
import { ApiError } from "../api/errors";
import type { TaskPatchInput } from "../api/hooks";
import { ErrorBanner } from "./ErrorBanner";
import styles from "./TaskEditDialog.module.css";

interface Props {
  task: Task;
  categories: Category[];
  saving?: boolean;
  /** Error from the last save attempt; a 409 offers to reload the server copy. */
  error: unknown;
  onSave: (input: Pick<TaskPatchInput, "fields" | "version">) => void;
  onDelete: (version: number) => void;
  onClose: () => void;
}

interface FormState {
  title: string;
  description: string;
  categoryId: string;
  priority: Priority;
  status: TaskStatus;
  dueDate: string;
}

function toForm(t: Task): FormState {
  return {
    title: t.title,
    description: t.description ?? "",
    categoryId: t.categoryId ?? "",
    priority: t.priority,
    status: t.status,
    dueDate: t.dueDate ?? "",
  };
}

export function TaskEditDialog({ task, categories, saving, error, onSave, onDelete, onClose }: Props) {
  // `base` is the server copy the form is based on; its version guards the save.
  const [base, setBase] = useState(task);
  const [form, setForm] = useState(() => toForm(task));
  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((f) => ({ ...f, [key]: value }));

  const conflict = error instanceof ApiError && error.isConflict ? (error.current as Task | undefined) : undefined;

  function submit(e: FormEvent) {
    e.preventDefault();
    if (!form.title.trim()) return;
    onSave({
      version: base.version,
      fields: {
        title: form.title.trim(),
        description: form.description.trim() || null,
        categoryId: form.categoryId || null,
        priority: form.priority,
        status: form.status,
        dueDate: form.dueDate || null,
      },
    });
  }

  function loadLatest() {
    if (!conflict) return;
    setBase(conflict);
    setForm(toForm(conflict));
  }

  return (
    <div className={styles.backdrop} onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="edit-task-heading"
        className={styles.dialog}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 id="edit-task-heading">Edit task</h2>
        {conflict ? (
          <div role="alert" className={styles.conflict}>
            <p>
              This task was changed elsewhere (now version {conflict.version}). Load the latest values, then re-apply
              your edit.
            </p>
            <button type="button" onClick={loadLatest}>
              Load latest
            </button>
          </div>
        ) : (
          <ErrorBanner error={error} />
        )}
        <form onSubmit={submit} className={styles.form}>
          <label>
            Title
            <input value={form.title} maxLength={200} required onChange={(e) => set("title", e.target.value)} />
          </label>
          <label>
            Description
            <textarea rows={3} value={form.description} maxLength={5000} onChange={(e) => set("description", e.target.value)} />
          </label>
          <div className={styles.row}>
            <label>
              Category
              <select value={form.categoryId} onChange={(e) => set("categoryId", e.target.value)}>
                <option value="">Inbox</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Priority
              <select value={form.priority} onChange={(e) => set("priority", e.target.value as Priority)}>
                <option value="LOW">Low</option>
                <option value="MEDIUM">Medium</option>
                <option value="HIGH">High</option>
                <option value="URGENT">Urgent</option>
              </select>
            </label>
            <label>
              Status
              <select value={form.status} onChange={(e) => set("status", e.target.value as TaskStatus)}>
                <option value="TODO">To do</option>
                <option value="IN_PROGRESS">In progress</option>
                <option value="DONE">Done</option>
              </select>
            </label>
            <label>
              Due date
              <input type="date" value={form.dueDate} onChange={(e) => set("dueDate", e.target.value)} />
            </label>
          </div>
          <div className={styles.actions}>
            <button type="button" className={styles.danger} onClick={() => onDelete(base.version)}>
              Delete
            </button>
            <span className={styles.spacer} />
            <button type="button" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className={styles.primary} disabled={saving || !form.title.trim()}>
              Save
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
