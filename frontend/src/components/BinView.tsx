import { useState } from "react";
import type { Category, Task } from "@ihelper/api-client";
import styles from "./BinView.module.css";

interface Props {
  tasks: Task[];
  categories: Category[];
  busy: boolean;
  onRestore: (task: Task) => void;
  /** Erases the chosen tasks for good. */
  onDeleteSelected: (ids: string[]) => void;
}

/**
 * Deleted tasks. They can be restored one by one, or erased: tick the tasks (or Select all) and choose
 * "Delete selected". Erasing is final, so it always asks first.
 */
export function BinView({ tasks, categories, busy, onRestore, onDeleteSelected }: Props) {
  const [ticked, setTicked] = useState<Set<string>>(new Set());
  const [confirming, setConfirming] = useState(false);
  const byId = new Map(categories.map((c) => [c.id, c]));

  // Only tasks still in the bin count: a restored or erased task silently drops out of the selection.
  const selectedIds = tasks.filter((t) => ticked.has(t.id)).map((t) => t.id);
  const allSelected = tasks.length > 0 && selectedIds.length === tasks.length;
  const count = selectedIds.length;

  function toggle(id: string) {
    setConfirming(false);
    setTicked((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleAll() {
    setConfirming(false);
    setTicked(allSelected ? new Set() : new Set(tasks.map((t) => t.id)));
  }

  return (
    <section aria-label="Bin">
      <div className={styles.head}>
        <h2>Bin</h2>
        {count > 0 &&
          (confirming ? (
            <span className={styles.confirm} role="group" aria-label="Confirm erasing">
              <span>
                Erase {count} {count === 1 ? "task" : "tasks"} forever?
              </span>
              <button
                type="button"
                className={styles.danger}
                disabled={busy}
                onClick={() => {
                  onDeleteSelected(selectedIds);
                  setConfirming(false);
                  setTicked(new Set());
                }}
              >
                Erase forever
              </button>
              <button type="button" onClick={() => setConfirming(false)}>
                Cancel
              </button>
            </span>
          ) : (
            <button type="button" className={styles.dangerText} disabled={busy} onClick={() => setConfirming(true)}>
              Delete selected ({count})
            </button>
          ))}
      </div>
      <p className={styles.hint}>
        Deleted tasks wait here until you erase them. Tick the tasks to delete (or Select all). Erasing cannot be
        undone.
      </p>
      {tasks.length === 0 ? (
        <p className={styles.empty}>The bin is empty.</p>
      ) : (
        <>
          <label className={styles.selectAll}>
            <input type="checkbox" checked={allSelected} onChange={toggleAll} />
            Select all
          </label>
          <ul className={styles.list}>
            {tasks.map((t) => {
              const category = t.categoryId ? byId.get(t.categoryId) : undefined;
              return (
                <li key={t.id} className={styles.item}>
                  <input
                    type="checkbox"
                    checked={ticked.has(t.id)}
                    aria-label={`Select ${t.title}`}
                    onChange={() => toggle(t.id)}
                  />
                  <div className={styles.body}>
                    <span className={styles.title}>{t.title}</span>
                    {category && <span className={styles.chip}>{category.name}</span>}
                  </div>
                  <button type="button" disabled={busy} aria-label={`Restore ${t.title}`} onClick={() => onRestore(t)}>
                    Restore
                  </button>
                </li>
              );
            })}
          </ul>
        </>
      )}
    </section>
  );
}
