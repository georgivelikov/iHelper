import { useState, type FormEvent } from "react";
import {
  closestCenter,
  DndContext,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import { arrayMove, rectSortingStrategy, SortableContext, sortableKeyboardCoordinates, useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
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
  /** Receives every category id in its new order. */
  onReorder: (ids: string[]) => void;
}

interface RowProps {
  category: Category;
  editing: boolean;
  select: React.ReactNode;
  editor: React.ReactNode;
  onEdit: () => void;
}

/** One category line: a drag handle, the entry itself and its edit button (or the inline editor). */
function SortableRow({ category, editing, select, editor, onEdit }: RowProps) {
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } = useSortable({
    id: category.id,
  });
  const style = { transform: CSS.Transform.toString(transform), transition };

  return (
    <li ref={setNodeRef} style={style} className={isDragging ? styles.dragging : undefined}>
      {editing ? (
        editor
      ) : (
        <div className={styles.row}>
          <button
            type="button"
            ref={setActivatorNodeRef}
            className={styles.handle}
            aria-label={`Reorder category ${category.name}`}
            {...attributes}
            {...listeners}
          >
            ⋮⋮
          </button>
          {select}
          <button type="button" className={styles.edit} aria-label={`Edit category ${category.name}`} onClick={onEdit}>
            ✎
          </button>
        </div>
      )}
    </li>
  );
}

export function CategorySidebar({
  categories,
  counts,
  selected,
  onSelect,
  onCreate,
  onUpdate,
  onDelete,
  onReorder,
}: Props) {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState("");
  const [color, setColor] = useState(DEFAULT_COLOR);
  const [newName, setNewName] = useState("");
  const [newColor, setNewColor] = useState(DEFAULT_COLOR);

  // A small movement threshold keeps a plain click on the handle from starting a drag.
  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

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

  function dragEnd({ active, over }: DragEndEvent) {
    if (!over || active.id === over.id) return;
    const ids = categories.map((c) => c.id);
    const from = ids.indexOf(String(active.id));
    const to = ids.indexOf(String(over.id));
    if (from < 0 || to < 0) return;
    onReorder(arrayMove(ids, from, to));
  }

  const item = (key: string, label: string, count: number | undefined, countLabel: string, dot?: string | null) => (
    <button
      type="button"
      className={`${styles.item} ${selected === key ? styles.selected : ""}`}
      aria-current={selected === key ? "true" : undefined}
      onClick={() => onSelect(key)}
    >
      {dot !== undefined && <span className={styles.dot} style={{ background: dot ?? "var(--muted)" }} />}
      <span className={styles.label}>{label}</span>
      <span className={styles.count} aria-label={`${count ?? 0} ${countLabel}`}>
        {count ?? 0}
      </span>
    </button>
  );

  return (
    <nav className={styles.sidebar} aria-label="Categories">
      <ul className={styles.list}>
        <li>{item("all", "Home", counts?.total, "open")}</li>
        <li>{item("inbox", "Inbox", counts?.inbox, "open")}</li>
      </ul>
      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={dragEnd}>
        <SortableContext items={categories.map((c) => c.id)} strategy={rectSortingStrategy}>
          <ul className={styles.list} aria-label="Your categories">
            {categories.map((c) => (
              <SortableRow
                key={c.id}
                category={c}
                editing={editingId === c.id}
                onEdit={() => startEdit(c)}
                select={item(c.id, c.name, counts?.byCategory?.[c.id], "open", c.color)}
                editor={
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
                }
              />
            ))}
          </ul>
        </SortableContext>
      </DndContext>
      <ul className={styles.list}>
        <li>{item("bin", "Bin", counts?.bin, "deleted")}</li>
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
