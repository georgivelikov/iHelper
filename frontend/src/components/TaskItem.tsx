import { useState } from "react";
import type { Category, Task } from "@ihelper/api-client";
import { isOverdue } from "../lib/dates";
import { PriorityBadge } from "./PriorityBadge";
import styles from "./TaskItem.module.css";

interface Props {
  task: Task;
  category?: Category;
  onToggle: (task: Task) => void;
  /** Opens the editor. */
  onEdit: (task: Task) => void;
  /** Moves the task to the bin. */
  onDelete: (task: Task) => void;
}

const stop = (e: { stopPropagation: () => void }) => e.stopPropagation();

export function TaskItem({ task, category, onToggle, onEdit, onDelete }: Props) {
  const [expanded, setExpanded] = useState(false);
  const done = task.status === "DONE";
  const overdue = isOverdue(task.dueDate, task.status);
  const className = [styles.item, done ? styles.done : "", overdue ? styles.overdue : ""].join(" ");

  // Clicking the row expands it to show the notes. The title stays a real button (so the keyboard
  // can toggle it; its click bubbles up to the row); the checkbox and the icons act on their own.
  return (
    <li className={className} data-priority={task.priority} data-overdue={overdue || undefined} onClick={() => setExpanded((open) => !open)}>
      <input
        type="checkbox"
        checked={done}
        aria-label={`${done ? "Reopen" : "Complete"} ${task.title}`}
        onClick={stop}
        onChange={() => onToggle(task)}
      />
      <div className={styles.body}>
        <button type="button" className={styles.title} aria-expanded={expanded}>
          {task.title}
        </button>
        <div className={styles.meta}>
          <PriorityBadge priority={task.priority} />
          {done && <span className={styles.doneChip}>Done</span>}
          {task.status === "IN_PROGRESS" && <span className={styles.chip}>In progress</span>}
          {category && (
            <span className={styles.chip}>
              <span className={styles.dot} style={{ background: category.color ?? "var(--muted)" }} />
              {category.name}
            </span>
          )}
          {task.dueDate && (
            <span className={overdue ? styles.dueOverdue : styles.due}>
              {overdue ? "Overdue · " : "Due "}
              {task.dueDate}
            </span>
          )}
        </div>
        {expanded && (
          <div className={styles.details} data-testid="task-details">
            {task.notes?.trim() ? (
              <p className={styles.notes}>{task.notes}</p>
            ) : (
              <p className={styles.noNotes}>No notes.</p>
            )}
          </div>
        )}
      </div>
      <div className={styles.actions}>
        <button
          type="button"
          className={styles.iconButton}
          aria-label={`Edit ${task.title}`}
          title="Edit"
          onClick={(e) => {
            stop(e);
            onEdit(task);
          }}
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M12 20h9" />
            <path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z" />
          </svg>
        </button>
        <button
          type="button"
          className={`${styles.iconButton} ${styles.delete}`}
          aria-label={`Delete ${task.title}`}
          title="Move to bin"
          onClick={(e) => {
            stop(e);
            onDelete(task);
          }}
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M3 6h18" />
            <path d="M8 6V4h8v2" />
            <path d="M19 6l-1 14H6L5 6" />
            <path d="M10 11v6M14 11v6" />
          </svg>
        </button>
      </div>
    </li>
  );
}
