import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { App } from "./App";
import { server } from "./test/setup";
import { createFakeApi, makeCategory, makeTask } from "./test/fakeApi";
import { renderWithClient } from "./test/render";

const work = makeCategory({ id: "cat-work", name: "Work" });
const home = makeCategory({ id: "cat-home", name: "Errands", color: "#22c55e" });

function setup(tasks = [
  makeTask({ id: "t1", title: "Ship release", priority: "URGENT", categoryId: work.id }),
  makeTask({ id: "t2", title: "Water plants", priority: "LOW", categoryId: home.id }),
  makeTask({ id: "t3", title: "Call dentist", priority: "HIGH" }),
  makeTask({ id: "t4", title: "Old chore", priority: "LOW", status: "DONE", categoryId: home.id }),
]) {
  const api = createFakeApi({ categories: [work, home], tasks });
  server.use(...api.handlers);
  renderWithClient(<App />);
  return api;
}

const sentTo = (api: ReturnType<typeof setup>, method: string, path: RegExp) =>
  api.state.requests.filter((r) => r.method === method && path.test(r.path));

describe("App", () => {
  it("lists tasks grouped by priority, finished ones included, with counts in the sidebar", async () => {
    setup();

    await screen.findByText("Ship release");
    expect(screen.getByText("Old chore")).toBeInTheDocument(); // done tasks stay visible by default

    const urgent = screen.getByRole("region", { name: "Urgent priority" });
    expect(within(urgent).getByText("Ship release")).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "High priority" })).toHaveTextContent("Call dentist");

    const nav = screen.getByRole("navigation", { name: "Categories" });
    expect(await within(nav).findByRole("button", { name: /^Home\s*3/ })).toBeInTheDocument();
    expect(within(nav).getByRole("button", { name: /^Inbox\s*1/ })).toBeInTheDocument();
    expect(within(nav).getByRole("button", { name: /^Work\s*1/ })).toBeInTheDocument();
  });

  it("adds a task with a client-generated id and shows it", async () => {
    const api = setup();
    await screen.findByText("Ship release");

    await userEvent.type(screen.getByLabelText("New task title"), "Book flights");
    await userEvent.selectOptions(screen.getByLabelText("New task priority"), "HIGH");
    await userEvent.click(screen.getByRole("button", { name: "Add" }));

    await screen.findByText("Book flights");
    const [post] = sentTo(api, "POST", /\/tasks$/);
    expect(post.body).toMatchObject({ title: "Book flights", priority: "HIGH" });
    expect(post.body?.id).toMatch(/^[0-9a-f-]{36}$/);
  });

  it("completing a task marks it Done and keeps it in the list", async () => {
    const api = setup();
    await screen.findByText("Call dentist");

    await userEvent.click(screen.getByRole("checkbox", { name: "Complete Call dentist" }));

    expect(await screen.findByRole("checkbox", { name: "Reopen Call dentist" })).toBeChecked();
    expect(screen.getByText("Call dentist")).toBeInTheDocument();
    const [patch] = sentTo(api, "PATCH", /\/tasks\/t3$/);
    expect(patch.body).toEqual({ status: "DONE", version: 0 });
    expect(sentTo(api, "DELETE", /\/tasks\//)).toHaveLength(0); // nothing is removed automatically
  });

  it("shows finished tasks below the open ones of the same priority", async () => {
    setup([
      makeTask({ id: "a", title: "Finished low", priority: "LOW", status: "DONE" }),
      makeTask({ id: "b", title: "Open low", priority: "LOW" }),
    ]);

    const low = await screen.findByRole("region", { name: "Low priority" });
    const titles = within(low).getAllByRole("listitem").map((li) => li.textContent ?? "");
    expect(titles[0]).toContain("Open low");
    expect(titles[1]).toContain("Finished low");
  });

  it("clicking a row expands it with the notes; the edit icon opens the editor", async () => {
    setup([makeTask({ id: "t1", title: "Ship release", priority: "URGENT", notes: "Tag it, then deploy" })]);
    await screen.findByText("Ship release");
    expect(screen.queryByText("Tag it, then deploy")).not.toBeInTheDocument();

    const row = within(screen.getByRole("region", { name: "Urgent priority" })).getByRole("listitem");
    await userEvent.click(within(row).getByText("Urgent")); // the badge, not the title

    expect(await within(row).findByText("Tag it, then deploy")).toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();

    await userEvent.click(within(row).getByRole("button", { name: "Edit Ship release" }));
    expect(await screen.findByRole("dialog")).toBeInTheDocument();
  });

  it("deleting a task moves it to the bin, where it can be restored", async () => {
    const api = setup();
    await screen.findByText("Call dentist");

    await userEvent.click(screen.getByRole("button", { name: "Delete Call dentist" }));

    await waitFor(() => expect(screen.queryByText("Call dentist")).not.toBeInTheDocument());
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument(); // the delete icon does not open the editor
    expect(sentTo(api, "DELETE", /\/tasks\/t3$/)).toHaveLength(1);

    const nav = screen.getByRole("navigation", { name: "Categories" });
    await userEvent.click(await within(nav).findByRole("button", { name: /^Bin\s*1/ }));
    expect(await screen.findByText("Call dentist")).toBeInTheDocument();
    expect(screen.queryByLabelText("New task title")).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Restore Call dentist" }));
    await waitFor(() => expect(screen.getByText("The bin is empty.")).toBeInTheDocument());
    expect(sentTo(api, "POST", /\/tasks\/t3\/restore$/)[0].body).toBeUndefined();

    await userEvent.click(within(nav).getByRole("button", { name: /^Home/ }));
    expect(await screen.findByText("Call dentist")).toBeInTheDocument();
  });

  it("Select all then Delete selected erases everything in the bin, after asking", async () => {
    const api = setup();
    await screen.findByText("Call dentist");
    await userEvent.click(screen.getByRole("button", { name: "Delete Call dentist" }));
    await userEvent.click(screen.getByRole("button", { name: "Delete Water plants" }));

    const nav = screen.getByRole("navigation", { name: "Categories" });
    await userEvent.click(await within(nav).findByRole("button", { name: /^Bin\s*2/ }));
    expect(screen.queryByRole("button", { name: "Empty bin" })).not.toBeInTheDocument(); // Select all covers it
    await userEvent.click(await screen.findByRole("checkbox", { name: "Select all" }));
    await userEvent.click(screen.getByRole("button", { name: "Delete selected (2)" }));

    expect(screen.getByText("Erase 2 tasks forever?")).toBeInTheDocument();
    expect(sentTo(api, "POST", /\/tasks\/bin\/purge$/)).toHaveLength(0); // nothing yet
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(sentTo(api, "POST", /\/tasks\/bin\/purge$/)).toHaveLength(0);

    await userEvent.click(screen.getByRole("button", { name: "Delete selected (2)" }));
    await userEvent.click(screen.getByRole("button", { name: "Erase forever" }));

    expect(await screen.findByText("The bin is empty.")).toBeInTheDocument();
    expect(sentTo(api, "POST", /\/tasks\/bin\/purge$/)).toHaveLength(1);
    await waitFor(() => expect(within(nav).getByRole("button", { name: /^Bin\s*0/ })).toBeInTheDocument());
  });

  it("selected tasks can be erased from the bin while the others stay", async () => {
    const api = setup();
    await screen.findByText("Call dentist");
    await userEvent.click(screen.getByRole("button", { name: "Delete Call dentist" }));
    await userEvent.click(screen.getByRole("button", { name: "Delete Water plants" }));
    await userEvent.click(screen.getByRole("button", { name: "Delete Ship release" }));
    const nav = screen.getByRole("navigation", { name: "Categories" });
    await userEvent.click(await within(nav).findByRole("button", { name: /^Bin\s*3/ }));
    await screen.findByRole("checkbox", { name: "Select Call dentist" });
    expect(screen.queryByRole("button", { name: /Delete selected/ })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole("checkbox", { name: "Select Call dentist" }));
    await userEvent.click(screen.getByRole("checkbox", { name: "Select Ship release" }));
    await userEvent.click(screen.getByRole("button", { name: "Delete selected (2)" }));

    expect(screen.getByText("Erase 2 tasks forever?")).toBeInTheDocument();
    expect(sentTo(api, "POST", /\/tasks\/bin\/purge$/)).toHaveLength(0); // nothing until confirmed
    await userEvent.click(screen.getByRole("button", { name: "Erase forever" }));

    await waitFor(() => expect(screen.queryByText("Call dentist")).not.toBeInTheDocument());
    expect(screen.queryByText("Ship release")).not.toBeInTheDocument();
    expect(screen.getByText("Water plants")).toBeInTheDocument(); // not ticked, still in the bin
    const [purge] = sentTo(api, "POST", /\/tasks\/bin\/purge$/);
    expect((purge.body?.ids as string[]).sort()).toEqual(["t1", "t3"]);
  });

  it("Select all ticks every task in the bin", async () => {
    setup();
    await screen.findByText("Call dentist");
    await userEvent.click(screen.getByRole("button", { name: "Delete Call dentist" }));
    await userEvent.click(screen.getByRole("button", { name: "Delete Water plants" }));
    const nav = screen.getByRole("navigation", { name: "Categories" });
    await userEvent.click(await within(nav).findByRole("button", { name: /^Bin\s*2/ }));

    await userEvent.click(await screen.findByRole("checkbox", { name: "Select all" }));

    expect(screen.getByRole("checkbox", { name: "Select Call dentist" })).toBeChecked();
    expect(screen.getByRole("checkbox", { name: "Select Water plants" })).toBeChecked();
    expect(screen.getByRole("button", { name: "Delete selected (2)" })).toBeInTheDocument();
  });

  it("deleting from the editor also moves the task to the bin", async () => {
    const api = setup([makeTask({ id: "t1", title: "Ship release" })]);
    await userEvent.click(await screen.findByRole("button", { name: "Edit Ship release" }));

    await userEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Delete" }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    expect(sentTo(api, "DELETE", /\/tasks\/t1$/)).toHaveLength(1);
    expect(api.state.bin.map((t) => t.id)).toEqual(["t1"]);
  });

  it("filters by status, priority and category through the API", async () => {
    const api = setup();
    await screen.findByText("Ship release");

    await userEvent.selectOptions(screen.getByLabelText("Show"), "done");
    await screen.findByText("Old chore");
    expect(screen.queryByText("Ship release")).not.toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Show"), "open");
    await waitFor(() => expect(screen.queryByText("Old chore")).not.toBeInTheDocument());
    await userEvent.selectOptions(screen.getByLabelText("Priority"), "LOW");
    await screen.findByText("Water plants");
    expect(screen.queryByText("Ship release")).not.toBeInTheDocument();

    await userEvent.selectOptions(screen.getByLabelText("Priority"), "");
    await screen.findByText("Ship release");
    const nav = screen.getByRole("navigation", { name: "Categories" });
    await userEvent.click(within(nav).getByRole("button", { name: /^Work/ }));
    await waitFor(() => expect(screen.queryByText("Water plants")).not.toBeInTheDocument());
    expect(screen.getByText("Ship release")).toBeInTheDocument();

    const listCalls = sentTo(api, "GET", /\/tasks$/);
    expect(listCalls.some((r) => r.search.getAll("status").join() === "DONE")).toBe(true);
    expect(listCalls.some((r) => r.search.get("categoryId") === "cat-work")).toBe(true);
  });

  it("asks the server for inbox tasks only", async () => {
    const api = setup();
    await screen.findByText("Ship release");

    const nav = screen.getByRole("navigation", { name: "Categories" });
    await userEvent.click(within(nav).getByRole("button", { name: /^Inbox/ }));

    await waitFor(() => expect(screen.queryByText("Ship release")).not.toBeInTheDocument());
    expect(screen.getByText("Call dentist")).toBeInTheDocument();
    expect(sentTo(api, "GET", /\/tasks$/).some((r) => r.search.get("inbox") === "true")).toBe(true);
  });

  it("edits a task in the dialog and clears the due date with null", async () => {
    const api = setup([
      makeTask({ id: "t1", title: "Ship release", priority: "HIGH", dueDate: "2999-01-01", version: 4 }),
    ]);
    await userEvent.click(await screen.findByRole("button", { name: "Edit Ship release" }));

    const dialog = screen.getByRole("dialog");
    const title = within(dialog).getByLabelText("Title");
    await userEvent.clear(title);
    await userEvent.type(title, "Ship v2");
    await userEvent.clear(within(dialog).getByLabelText("Due date"));
    await userEvent.click(within(dialog).getByRole("button", { name: "Save" }));

    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    await screen.findByText("Ship v2");
    const [patch] = sentTo(api, "PATCH", /\/tasks\/t1$/);
    expect(patch.body).toMatchObject({ title: "Ship v2", dueDate: null, categoryId: null, version: 4 });
  });

  it("offers the latest server copy when saving hits a 409", async () => {
    const api = setup([makeTask({ id: "t1", title: "Ship release", version: 0 })]);
    api.state.forceConflict.set("t1", makeTask({ id: "t1", title: "Ship release (edited elsewhere)", version: 3 }));

    await userEvent.click(await screen.findByRole("button", { name: "Edit Ship release" }));
    const dialog = screen.getByRole("dialog");
    await userEvent.click(within(dialog).getByRole("button", { name: "Save" }));

    await within(dialog).findByText(/changed elsewhere \(now version 3\)/);
    await userEvent.click(within(dialog).getByRole("button", { name: "Load latest" }));
    expect(within(dialog).getByLabelText("Title")).toHaveValue("Ship release (edited elsewhere)");

    await userEvent.click(within(dialog).getByRole("button", { name: "Save" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).not.toBeInTheDocument());
    const patches = sentTo(api, "PATCH", /\/tasks\/t1$/);
    expect(patches.at(-1)?.body?.version).toBe(3);
  });

  it("shows server validation errors from Problem Details", async () => {
    const api = setup();
    api.state.rejectCreate = { field: "title", message: "size must be between 0 and 200" };
    await screen.findByText("Ship release");

    await userEvent.type(screen.getByLabelText("New task title"), "x");
    await userEvent.click(screen.getByRole("button", { name: "Add" }));

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent("Validation failed");
    expect(alert).toHaveTextContent("title: size must be between 0 and 200");
  });

  it("creates, renames and deletes a category", async () => {
    const api = setup();
    await screen.findByText("Ship release");

    await userEvent.type(screen.getByLabelText("New category name"), "Garden");
    await userEvent.click(screen.getByRole("button", { name: "Add category" }));
    const nav = screen.getByRole("navigation", { name: "Categories" });
    await within(nav).findByRole("button", { name: /^Garden/ });

    await userEvent.click(within(nav).getByRole("button", { name: "Edit category Garden" }));
    const name = within(nav).getByLabelText("Category name");
    await userEvent.clear(name);
    await userEvent.type(name, "Yard");
    await userEvent.click(within(nav).getByRole("button", { name: "Save" }));
    await within(nav).findByRole("button", { name: /^Yard/ });

    await userEvent.click(within(nav).getByRole("button", { name: "Edit category Yard" }));
    await userEvent.click(within(nav).getByRole("button", { name: "Delete" }));
    await waitFor(() => expect(within(nav).queryByRole("button", { name: /^Yard/ })).not.toBeInTheDocument());

    expect(sentTo(api, "PUT", /\/categories\//)[0].body).toMatchObject({ name: "Yard", version: 0 });
    expect(sentTo(api, "DELETE", /\/categories\//)).toHaveLength(1);
  });

  it("shows an empty state", async () => {
    setup([]);
    expect(await screen.findByText("No tasks here.")).toBeInTheDocument();
  });
});
