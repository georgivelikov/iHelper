import type { Category, Task } from "@ihelper/api-client";
import { isOverdue } from "../lib/dates";
import { PriorityBadge } from "./PriorityBadge";
import styles from "./TaskItem.module.css";

interface Props {
  task: Task;
  category?: Category;
  onToggle: (task: Task) => void;
  onEdit: (task: Task) => void;
}

export function TaskItem({ task, category, onToggle, onEdit }: Props) {
  const done = task.status === "DONE";
  const overdue = isOverdue(task.dueDate, task.status);
  const className = [styles.item, done ? styles.done : "", overdue ? styles.overdue : ""].join(" ");

  return (
    <li className={className} data-overdue={overdue || undefined}>
      <input
        type="checkbox"
        checked={done}
        aria-label={`${done ? "Reopen" : "Complete"} ${task.title}`}
        onChange={() => onToggle(task)}
      />
      <div className={styles.body}>
        <button type="button" className={styles.title} onClick={() => onEdit(task)}>
          {task.title}
        </button>
        <div className={styles.meta}>
          <PriorityBadge priority={task.priority} />
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
    </li>
  );
}
