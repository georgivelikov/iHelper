import { useState } from "react";
import type { Category, Task } from "@ihelper/api-client";
import styles from "./BinView.module.css";

interface Props {
  tasks: Task[];
  categories: Category[];
  busy: boolean;
  onRestore: (task: Task) => void;
  /** Erases every task in the bin. */
  onEmpty: () => void;
  /** Erases only the chosen tasks. */
  onDeleteSelected: (ids: string[]) => void;
}

type Confirming = "all" | "selected" | null;

/**
 * Deleted tasks. They can be restored one by one, erased selectively (tick them, then "Delete selected"),
 * or all at once by emptying the bin. Erasing is final, so it always asks first.
 */
export function BinView({ tasks, categories, busy, onRestore, onEmpty, onDeleteSelected }: Props) {
  const [ticked, setTicked] = useState<Set<string>>(new Set());
  const [confirming, setConfirming] = useState<Confirming>(null);
  const byId = new Map(categories.map((c) => [c.id, c]));

  // Only tasks still in the bin count: a restored or erased task silently drops out of the selection.
  const selectedIds = tasks.filter((t) => ticked.has(t.id)).map((t) => t.id);
  const allSelected = tasks.length > 0 && selectedIds.length === tasks.length;

  function toggle(id: string) {
    setConfirming(null);
    setTicked((current) => {
      const next = new Set(current);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function toggleAll() {
    setConfirming(null);
    setTicked(allSelected ? new Set() : new Set(tasks.map((t) => t.id)));
  }

  const count = confirming === "all" ? tasks.length : selectedIds.length;
  const plural = (n: number) => (n === 1 ? "task" : "tasks");

  return (
    <section aria-label="Bin">
      <div className={styles.head}>
        <h2>Bin</h2>
        {tasks.length > 0 &&
          (confirming ? (
            <span className={styles.confirm} role="group" aria-label="Confirm erasing">
              <span>
                Erase {count} {plural(count)} forever?
              </span>
              <button
                type="button"
                className={styles.danger}
                disabled={busy}
                onClick={() => {
                  if (confirming === "all") onEmpty();
                  else onDeleteSelected(selectedIds);
                  setConfirming(null);
                  setTicked(new Set());
                }}
              >
                Erase forever
              </button>
              <button type="button" onClick={() => setConfirming(null)}>
                Cancel
              </button>
            </span>
          ) : (
            <span className={styles.actions}>
              {selectedIds.length > 0 && (
                <button type="button" className={styles.dangerText} disabled={busy} onClick={() => setConfirming("selected")}>
                  Delete selected ({selectedIds.length})
                </button>
              )}
              <button type="button" disabled={busy} onClick={() => setConfirming("all")}>
                Empty bin
              </button>
            </span>
          ))}
      </div>
      <p className={styles.hint}>
        Deleted tasks wait here until you erase them. Tick tasks to delete only those, or empty the whole bin. Erasing
        cannot be undone.
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
