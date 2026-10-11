import { useState } from "react";
import type { Category, Task } from "@ihelper/api-client";
import styles from "./BinView.module.css";

interface Props {
  tasks: Task[];
  categories: Category[];
  busy: boolean;
  onRestore: (task: Task) => void;
  onEmpty: () => void;
}

/** Deleted tasks. They can be restored one by one, or erased for good by emptying the bin. */
export function BinView({ tasks, categories, busy, onRestore, onEmpty }: Props) {
  const [confirming, setConfirming] = useState(false);
  const byId = new Map(categories.map((c) => [c.id, c]));

  return (
    <section aria-label="Bin">
      <div className={styles.head}>
        <h2>Bin</h2>
        {tasks.length > 0 &&
          (confirming ? (
            <span className={styles.confirm} role="group" aria-label="Confirm emptying the bin">
              <span>
                Erase {tasks.length} {tasks.length === 1 ? "task" : "tasks"} forever?
              </span>
              <button
                type="button"
                className={styles.danger}
                disabled={busy}
                onClick={() => {
                  onEmpty();
                  setConfirming(false);
                }}
              >
                Erase forever
              </button>
              <button type="button" onClick={() => setConfirming(false)}>
                Cancel
              </button>
            </span>
          ) : (
            <button type="button" onClick={() => setConfirming(true)}>
              Empty bin
            </button>
          ))}
      </div>
      <p className={styles.hint}>Deleted tasks wait here until you empty the bin. Emptying it cannot be undone.</p>
      {tasks.length === 0 ? (
        <p className={styles.empty}>The bin is empty.</p>
      ) : (
        <ul className={styles.list}>
          {tasks.map((t) => {
            const category = t.categoryId ? byId.get(t.categoryId) : undefined;
            return (
              <li key={t.id} className={styles.item}>
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
      )}
    </section>
  );
}
