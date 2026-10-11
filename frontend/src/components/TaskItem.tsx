import type { Category, Task } from "@ihelper/api-client";
import { isOverdue } from "../lib/dates";
import { PriorityBadge } from "./PriorityBadge";
import styles from "./TaskItem.module.css";

interface Props {
  task: Task;
  category?: Category;
  onToggle: (task: Task) => void;
  onEdit: (task: Task) => void;
  /** Moves the task to the bin. */
  onDelete: (task: Task) => void;
}

export function TaskItem({ task, category, onToggle, onEdit, onDelete }: Props) {
  const done = task.status === "DONE";
  const overdue = isOverdue(task.dueDate, task.status);
  const className = [styles.item, done ? styles.done : "", overdue ? styles.overdue : ""].join(" ");

  // The whole row opens the editor. The title stays a real button so it is reachable by keyboard
  // (its click bubbles up to the row); the checkbox and delete button must not open it.
  return (
    <li className={className} data-overdue={overdue || undefined} onClick={() => onEdit(task)}>
      <input
        type="checkbox"
        checked={done}
        aria-label={`${done ? "Reopen" : "Complete"} ${task.title}`}
        onClick={(e) => e.stopPropagation()}
        onChange={() => onToggle(task)}
      />
      <div className={styles.body}>
        <button type="button" className={styles.title}>
          {task.title}
        </button>
        <div className={styles.meta}>
          <PriorityBadge priority={task.priority} />
          {done && <span className={styles.chip}>Done</span>}
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
      </div>
      <button
        type="button"
        className={styles.delete}
        aria-label={`Delete ${task.title}`}
        title="Move to bin"
        onClick={(e) => {
          e.stopPropagation();
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
    </li>
  );
}
