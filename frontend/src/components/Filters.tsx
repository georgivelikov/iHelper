import type { Priority } from "@ihelper/api-client";
import type { SortKey, StatusFilter } from "../api/hooks";
import styles from "./Filters.module.css";

interface Props {
  status: StatusFilter;
  priority: Priority | "";
  sort: SortKey;
  q: string;
  onStatus: (s: StatusFilter) => void;
  onPriority: (p: Priority | "") => void;
  onSort: (s: SortKey) => void;
  onSearch: (q: string) => void;
}

export function Filters({ status, priority, sort, q, onStatus, onPriority, onSort, onSearch }: Props) {
  return (
    <div className={styles.filters} role="search">
      <input
        type="search"
        aria-label="Search tasks"
        placeholder="Search…"
        value={q}
        onChange={(e) => onSearch(e.target.value)}
      />
      <label>
        Show
        <select value={status} onChange={(e) => onStatus(e.target.value as StatusFilter)}>
          <option value="open">Open</option>
          <option value="done">Done</option>
          <option value="all">All</option>
        </select>
      </label>
      <label>
        Priority
        <select value={priority} onChange={(e) => onPriority(e.target.value as Priority | "")}>
          <option value="">Any</option>
          <option value="URGENT">Urgent</option>
          <option value="HIGH">High</option>
          <option value="MEDIUM">Medium</option>
          <option value="LOW">Low</option>
        </select>
      </label>
      <label>
        Sort by
        <select value={sort} onChange={(e) => onSort(e.target.value as SortKey)}>
          <option value="priority">Priority</option>
          <option value="due">Due date</option>
          <option value="newest">Newest</option>
          <option value="title">Title</option>
        </select>
      </label>
    </div>
  );
}
