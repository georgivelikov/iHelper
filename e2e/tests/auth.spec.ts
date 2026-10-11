import { randomUUID } from "node:crypto";
import { AuthPage } from "../pages/AuthPage";
import { TasksPage } from "../pages/TasksPage";
import { PASSWORD, expect, test } from "../fixtures";

const newName = () => `ui${randomUUID().replace(/-/g, "").slice(0, 16)}`;

test.describe("accounts", () => {
  test("visitors see the sign-in form, not the app", async ({ page }) => {
    const auth = new AuthPage(page);
    await auth.goto();

    await auth.expectSignedOut();
    await expect(page.getByLabel("New task title")).toHaveCount(0);
  });

  test("create an account, start with the five categories, sign out and back in", async ({ page }) => {
    const auth = new AuthPage(page);
    const tasks = new TasksPage(page);
    const username = newName();
    await auth.goto();

    await auth.register(username, PASSWORD, process.env.E2E_INVITE_CODE ?? "");
    await auth.expectSignedInAs(username);
    for (const name of ["Personal", "Work", "Home", "Shopping", "Ideas"]) {
      await expect(tasks.category(name)).toBeVisible();
    }

    await auth.signOut();
    await auth.expectSignedOut();

    await auth.signIn(username, PASSWORD);
    await auth.expectSignedInAs(username);
  });

  test("a wrong password is refused with a message", async ({ page, account }) => {
    const auth = new AuthPage(page);
    // `account` exists, but this page has no cookie of its own after we clear it
    await page.context().clearCookies();
    await auth.goto();
    await auth.expectSignedOut();

    await auth.signIn(account.username, "definitely-not-it");

    await expect(auth.alert).toContainText("Invalid username or password");
    await auth.expectSignedOut();
  });

  test("the session survives a reload and ends on sign-out", async ({ page, account, api }) => {
    const auth = new AuthPage(page);
    const tasks = new TasksPage(page);
    const title = api.uniq("Remember me");
    await api.createTask({ title });

    await tasks.goto();
    await expect(tasks.task(title)).toBeVisible();
    await page.reload();
    await auth.expectSignedInAs(account.username);
    await expect(tasks.task(title)).toBeVisible();

    await auth.signOut();
    await auth.expectSignedOut();
    await page.reload();
    await auth.expectSignedOut(); // the cookie was revoked, not just hidden
  });

  test("another user never sees my tasks", async ({ browser, api }) => {
    const title = api.uniq("Private");
    await api.createTask({ title });

    const stranger = await browser.newContext({ baseURL: test.info().project.use.baseURL });
    const page = await stranger.newPage();
    const auth = new AuthPage(page);
    const tasks = new TasksPage(page);
    await auth.goto();
    await auth.register(newName(), PASSWORD, process.env.E2E_INVITE_CODE ?? "");
    await expect(page.getByLabel("New task title")).toBeVisible();

    await tasks.search(title);
    await expect(tasks.task(title)).toHaveCount(0);
    await stranger.close();
  });
});
