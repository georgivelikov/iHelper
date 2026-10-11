import { ApiError } from "../api/errors";
import styles from "./ErrorBanner.module.css";

function describe(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.isConflict) {
      return "This item was changed elsewhere in the meantime. The list has been refreshed.";
    }
    const { title, detail } = error.problem;
    return title && detail && title !== detail ? `${title}: ${detail}` : error.message;
  }
  return error instanceof Error ? error.message : "Something went wrong";
}

/** Renders an API Problem Details error (title, detail, per-field messages) or any other error. */
export function ErrorBanner({ error, onDismiss }: { error: unknown; onDismiss?: () => void }) {
  if (!error) return null;
  const fields = error instanceof ApiError ? (error.problem.errors ?? []) : [];

  return (
    <div role="alert" className={styles.banner}>
      <div>
        <strong>{describe(error)}</strong>
        {fields.length > 0 && (
          <ul>
            {fields.map((f) => (
              <li key={f.field}>
                {f.field}: {f.message}
              </li>
            ))}
          </ul>
        )}
      </div>
      {onDismiss && (
        <button type="button" onClick={onDismiss} aria-label="Dismiss error">
          ×
        </button>
      )}
    </div>
  );
}
