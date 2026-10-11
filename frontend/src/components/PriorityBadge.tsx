import type { Priority } from "@ihelper/api-client";
import styles from "./PriorityBadge.module.css";

const LABEL: Record<Priority, string> = {
  LOW: "Low",
  MEDIUM: "Medium",
  HIGH: "High",
  URGENT: "Urgent",
};

export function PriorityBadge({ priority }: { priority: Priority }) {
  return (
    <span className={`${styles.badge} ${styles[priority.toLowerCase()]}`} data-priority={priority}>
      {LABEL[priority]}
    </span>
  );
}
