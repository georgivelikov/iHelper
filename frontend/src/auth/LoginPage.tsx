import { useState, type FormEvent } from "react";
import { ApiError } from "../api/errors";
import { register, signIn } from "./session";
import styles from "./LoginPage.module.css";

type Mode = "signin" | "register";

function messageOf(error: unknown): string {
  if (error instanceof ApiError) {
    const fields = error.problem.errors ?? [];
    if (fields.length > 0) return fields.map((f) => `${f.field}: ${f.message}`).join("; ");
    if (error.status === 429) return "Too many attempts. Please wait a few minutes and try again.";
    return error.problem.detail ?? error.message;
  }
  return error instanceof Error ? error.message : "Something went wrong";
}

export function LoginPage() {
  const [mode, setMode] = useState<Mode>("signin");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [inviteCode, setInviteCode] = useState("");
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (mode === "signin") await signIn(username.trim(), password);
      else await register(username.trim(), password, inviteCode);
    } catch (err) {
      setError(err);
      setBusy(false);
    }
    // On success the session changes and this page unmounts.
  }

  const registering = mode === "register";

  return (
    <main className={styles.page}>
      <form className={styles.card} onSubmit={submit}>
        <h1>
          <img src="/logo.svg" alt="" width="32" height="32" />
          iHelper
        </h1>
        <h2>{registering ? "Create account" : "Sign in"}</h2>

        {error !== null && (
          <div role="alert" className={styles.error}>
            {messageOf(error)}
          </div>
        )}

        <label>
          Username
          <input
            autoComplete="username"
            value={username}
            maxLength={50}
            required
            onChange={(e) => setUsername(e.target.value)}
          />
        </label>
        <label>
          Password
          <input
            type="password"
            autoComplete={registering ? "new-password" : "current-password"}
            value={password}
            maxLength={72}
            minLength={registering ? 10 : undefined}
            required
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {registering && (
          <>
            <p className={styles.hint}>At least 10 characters.</p>
            <label>
              Invite code
              <input autoComplete="off" value={inviteCode} onChange={(e) => setInviteCode(e.target.value)} />
            </label>
          </>
        )}

        <button type="submit" className={styles.primary} disabled={busy}>
          {registering ? "Create account" : "Sign in"}
        </button>
        <button
          type="button"
          className={styles.link}
          onClick={() => {
            setMode(registering ? "signin" : "register");
            setError(null);
          }}
        >
          {registering ? "I already have an account" : "Create an account"}
        </button>
      </form>
    </main>
  );
}
