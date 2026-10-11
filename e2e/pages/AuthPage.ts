import { expect, type Locator, type Page } from "@playwright/test";

/** Sign-in / create-account screen and the signed-in header. */
export class AuthPage {
  readonly alert: Locator;

  constructor(readonly page: Page) {
    this.alert = page.getByRole("alert");
  }

  async goto() {
    await this.page.goto("/");
  }

  async expectSignedOut() {
    await expect(this.page.getByRole("heading", { name: "Sign in" })).toBeVisible();
  }

  async expectSignedInAs(username: string) {
    await expect(this.page.getByRole("heading", { name: "iHelper" })).toBeVisible();
    await expect(this.page.getByText(username, { exact: true })).toBeVisible();
  }

  async signIn(username: string, password: string) {
    await this.page.getByLabel("Username").fill(username);
    await this.page.getByLabel("Password").fill(password);
    await this.page.getByRole("button", { name: "Sign in" }).click();
  }

  async register(username: string, password: string, inviteCode = "") {
    await this.page.getByRole("button", { name: "Create an account" }).click();
    await this.page.getByLabel("Username").fill(username);
    await this.page.getByLabel("Password").fill(password);
    if (inviteCode) await this.page.getByLabel("Invite code").fill(inviteCode);
    await this.page.getByRole("button", { name: "Create account" }).click();
  }

  async signOut() {
    await this.page.getByRole("button", { name: "Sign out" }).click();
  }
}
