import { useState, type FormEvent } from "react";
import type { Category, Priority } from "@ihelper/api-client";
import styles from "./QuickAdd.module.css";

const PRIORITIES: Priority[] = ["LOW", "MEDIUM", "HIGH", "URGENT"];

interface Props {
  categories: Category[];
  /** Category preselected from the sidebar; "" means Inbox. */
  defaultCategoryId: string;
  disabled?: boolean;
  onAdd: (task: { title: string; priority: Priority; categoryId?: string }) => void;
}

export function QuickAdd({ categories, defaultCategoryId, disabled, onAdd }: Props) {
  const [title, setTitle] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [categoryId, setCategoryId] = useState<string | null>(null);
  const effectiveCategory = categoryId ?? defaultCategoryId;

  function submit(e: FormEvent) {
    e.preventDefault();
    const trimmed = title.trim();
    if (!trimmed) return;
    onAdd({ title: trimmed, priority, categoryId: effectiveCategory || undefined });
    setTitle("");
  }

  return (
    <form className={styles.form} onSubmit={submit}>
      <input
        className={styles.title}
        aria-label="New task title"
        placeholder="Add a task…"
        value={title}
        maxLength={200}
        onChange={(e) => setTitle(e.target.value)}
      />
      <select aria-label="New task priority" value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
        {PRIORITIES.map((p) => (
          <option key={p} value={p}>
            {p.charAt(0) + p.slice(1).toLowerCase()}
          </option>
        ))}
      </select>
      <select aria-label="New task category" value={effectiveCategory} onChange={(e) => setCategoryId(e.target.value)}>
        <option value="">Inbox</option>
        {categories.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </select>
      <button type="submit" className="primary-button" disabled={disabled || !title.trim()}>
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" aria-hidden="true">
          <path d="M12 5v14M5 12h14" />
        </svg>
        Add
      </button>
    </form>
  );
}
