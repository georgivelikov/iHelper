import type { Category, Priority, Task } from "@ihelper/api-client";
import type { SortKey } from "../api/hooks";
import { TaskItem } from "./TaskItem";
import styles from "./TaskList.module.css";

const GROUP_ORDER: Priority[] = ["URGENT", "HIGH", "MEDIUM", "LOW"];
const GROUP_LABEL: Record<Priority, string> = {
  URGENT: "Urgent",
  HIGH: "High",
  MEDIUM: "Medium",
  LOW: "Low",
};

interface Props {
  tasks: Task[];
  categories: Category[];
  sort: SortKey;
  onToggle: (task: Task) => void;
  onEdit: (task: Task) => void;
}

export function TaskList({ tasks, categories, sort, onToggle, onEdit }: Props) {
  if (tasks.length === 0) {
    return <p className={styles.empty}>No tasks here.</p>;
  }
  const byId = new Map(categories.map((c) => [c.id, c]));
  const renderItem = (t: Task) => (
    <TaskItem key={t.id} task={t} category={t.categoryId ? byId.get(t.categoryId) : undefined} onToggle={onToggle} onEdit={onEdit} />
  );

  // Grouped by priority when sorted by priority; other sorts are a flat list.
  if (sort !== "priority") {
    return <ul className={styles.list}>{tasks.map(renderItem)}</ul>;
  }
  return (
    <div>
      {GROUP_ORDER.map((p) => {
        const group = tasks.filter((t) => t.priority === p);
        if (group.length === 0) return null;
        return (
          <section key={p} aria-label={`${GROUP_LABEL[p]} priority`}>
            <h3 className={styles.heading}>
              {GROUP_LABEL[p]} <span>{group.length}</span>
            </h3>
            <ul className={styles.list}>{group.map(renderItem)}</ul>
          </section>
        );
      })}
    </div>
  );
}
