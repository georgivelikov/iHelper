import { useState, type FormEvent } from "react";
import type { Category, TaskCounts } from "@ihelper/api-client";
import styles from "./CategorySidebar.module.css";

const DEFAULT_COLOR = "#6366f1";

interface Props {
  categories: Category[];
  counts: TaskCounts | undefined;
  selected: string;
  onSelect: (view: string) => void;
  onCreate: (c: { name: string; color: string }) => void;
  onUpdate: (c: { id: string; version: number; name: string; color: string }) => void;
  onDelete: (c: { id: string; version: number }) => void;
}

export function CategorySidebar({ categories, counts, selected, onSelect, onCreate, onUpdate, onDelete }: Props) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [color, setColor] = useState(DEFAULT_COLOR);
  const [newName, setNewName] = useState("");
  const [newColor, setNewColor] = useState(DEFAULT_COLOR);

  function startEdit(c: Category) {
    setEditingId(c.id);
    setName(c.name);
    setColor(c.color ?? DEFAULT_COLOR);
  }

  function saveEdit(e: FormEvent, c: Category) {
    e.preventDefault();
    if (!name.trim()) return;
    onUpdate({ id: c.id, version: c.version, name: name.trim(), color });
    setEditingId(null);
  }

  function add(e: FormEvent) {
    e.preventDefault();
    if (!newName.trim()) return;
    onCreate({ name: newName.trim(), color: newColor });
    setNewName("");
  }

  const item = (key: string, label: string, count: number | undefined, dot?: string | null) => (
    <button
      type="button"
      className={`${styles.item} ${selected === key ? styles.selected : ""}`}
      aria-current={selected === key ? "true" : undefined}
      onClick={() => onSelect(key)}
    >
      {dot !== undefined && <span className={styles.dot} style={{ background: dot ?? "var(--muted)" }} />}
      <span className={styles.label}>{label}</span>
      <span className={styles.count} aria-label={`${count ?? 0} open`}>
        {count ?? 0}
      </span>
    </button>
  );

  return (
    <nav className={styles.sidebar} aria-label="Categories">
      <ul className={styles.list}>
        <li>{item("all", "All", counts?.total)}</li>
        <li>{item("inbox", "Inbox", counts?.inbox)}</li>
        {categories.map((c) => (
          <li key={c.id}>
            {editingId === c.id ? (
              <form className={styles.editor} onSubmit={(e) => saveEdit(e, c)}>
                <input aria-label="Category name" value={name} maxLength={100} onChange={(e) => setName(e.target.value)} />
                <input type="color" aria-label="Category color" value={color} onChange={(e) => setColor(e.target.value)} />
                <button type="submit">Save</button>
                <button type="button" onClick={() => setEditingId(null)}>
                  Cancel
                </button>
                <button
                  type="button"
                  className={styles.danger}
                  onClick={() => {
                    onDelete({ id: c.id, version: c.version });
                    setEditingId(null);
                  }}
                >
                  Delete
                </button>
              </form>
            ) : (
              <div className={styles.row}>
                {item(c.id, c.name, counts?.byCategory?.[c.id], c.color)}
                <button type="button" className={styles.edit} aria-label={`Edit category ${c.name}`} onClick={() => startEdit(c)}>
                  ✎
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
      <form className={styles.add} onSubmit={add}>
        <input aria-label="New category name" placeholder="New category" value={newName} maxLength={100} onChange={(e) => setNewName(e.target.value)} />
        <input type="color" aria-label="New category color" value={newColor} onChange={(e) => setNewColor(e.target.value)} />
        <button type="submit" disabled={!newName.trim()}>
          Add category
        </button>
      </form>
    </nav>
  );
}
