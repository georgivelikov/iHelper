import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { makeCategory } from "../test/fakeApi";
import { QuickAdd } from "./QuickAdd";

const categories = [makeCategory({ id: "c1", name: "Work" }), makeCategory({ id: "c2", name: "Home" })];

describe("QuickAdd", () => {
  it("submits title, priority and category, then clears the title", async () => {
    const onAdd = vi.fn();
    render(<QuickAdd categories={categories} defaultCategoryId="" onAdd={onAdd} />);

    await userEvent.type(screen.getByLabelText("New task title"), "  Buy milk ");
    await userEvent.selectOptions(screen.getByLabelText("New task priority"), "HIGH");
    await userEvent.selectOptions(screen.getByLabelText("New task category"), "c2");
    await userEvent.click(screen.getByRole("button", { name: "Add" }));

    expect(onAdd).toHaveBeenCalledWith({ title: "Buy milk", priority: "HIGH", categoryId: "c2" });
    expect(screen.getByLabelText("New task title")).toHaveValue("");
  });

  it("defaults to the category selected in the sidebar", async () => {
    const onAdd = vi.fn();
    render(<QuickAdd categories={categories} defaultCategoryId="c1" onAdd={onAdd} />);

    await userEvent.type(screen.getByLabelText("New task title"), "Standup notes{Enter}");

    expect(onAdd).toHaveBeenCalledWith({ title: "Standup notes", priority: "MEDIUM", categoryId: "c1" });
  });

  it("sends no category for Inbox", async () => {
    const onAdd = vi.fn();
    render(<QuickAdd categories={categories} defaultCategoryId="" onAdd={onAdd} />);

    await userEvent.type(screen.getByLabelText("New task title"), "Loose thought{Enter}");

    expect(onAdd).toHaveBeenCalledWith({ title: "Loose thought", priority: "MEDIUM", categoryId: undefined });
  });

  it("will not add a blank title", async () => {
    const onAdd = vi.fn();
    render(<QuickAdd categories={categories} defaultCategoryId="" onAdd={onAdd} />);

    await userEvent.type(screen.getByLabelText("New task title"), "   {Enter}");

    expect(screen.getByRole("button", { name: "Add" })).toBeDisabled();
    expect(onAdd).not.toHaveBeenCalled();
  });
});
