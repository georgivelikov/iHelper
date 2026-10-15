/** Today as YYYY-MM-DD in the user's local timezone (due dates are plain local dates). */
export function todayIso(now: Date = new Date()): string {
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/** ISO dates compare correctly as strings. */
export function isOverdue(dueDate: string | null | undefined, status: string, today: string = todayIso()): boolean {
  return !!dueDate && status !== "DONE" && dueDate < today;
}

/** Overdue, or due today: the cases that should glow red. */
export function isDueOrOverdue(dueDate: string | null | undefined, status: string, today: string = todayIso()): boolean {
  return !!dueDate && status !== "DONE" && dueDate <= today;
}
