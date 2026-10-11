import { useEffect, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import type { User } from "@ihelper/api-client";
import { LoginPage } from "./LoginPage";
import { restoreSession } from "./session";
import { useSession } from "./useSession";

/** Shows the sign-in screen until there is a session; renders the app with the signed-in user after. */
export function AuthGate({ children }: { children: (user: User) => ReactNode }) {
  const session = useSession();
  const queryClient = useQueryClient();
  const userId = session.status === "authenticated" ? session.user.id : null;

  useEffect(() => {
    void restoreSession();
  }, []);

  // Never show one account's cached data to the next one that signs in on this browser.
  // Signing in as someone else always passes through "anonymous", when the app is unmounted.
  useEffect(() => {
    if (userId === null) queryClient.clear();
  }, [userId, queryClient]);

  if (session.status === "loading") return <p role="status">Loading…</p>;
  if (session.status === "anonymous") return <LoginPage />;
  return <>{children(session.user)}</>;
}
