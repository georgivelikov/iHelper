import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { makeCategory, makeTask } from "../test/fakeApi";
import { TaskItem } from "./TaskItem";

const noop = () => {};

describe("TaskItem", () => {
  it("highlights an open task whose due date has passed", () => {
    const task = makeTask({ id: "1", title: "Pay rent", dueDate: "2000-01-01" });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={noop} onDelete={noop} /></ul>);

    expect(screen.getByRole("listitem")).toHaveAttribute("data-overdue", "true");
    expect(screen.getByText(/Overdue · 2000-01-01/)).toBeInTheDocument();
  });

  it("does not highlight a finished task even if the date passed", () => {
    const task = makeTask({ id: "1", title: "Pay rent", dueDate: "2000-01-01", status: "DONE" });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={noop} onDelete={noop} /></ul>);

    expect(screen.getByRole("listitem")).not.toHaveAttribute("data-overdue");
    expect(screen.getByRole("checkbox")).toBeChecked();
  });

  it("does not highlight a future due date", () => {
    const task = makeTask({ id: "1", title: "Later", dueDate: "2999-01-01" });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={noop} onDelete={noop} /></ul>);

    expect(screen.getByRole("listitem")).not.toHaveAttribute("data-overdue");
    expect(screen.getByText("Due 2999-01-01")).toBeInTheDocument();
  });

  it("shows priority and category and reports toggle and edit", async () => {
    const onToggle = vi.fn();
    const onEdit = vi.fn();
    const task = makeTask({ id: "1", title: "Write report", priority: "URGENT", categoryId: "c1" });
    render(
      <ul>
        <TaskItem task={task} category={makeCategory({ id: "c1", name: "Work" })} onToggle={onToggle} onEdit={onEdit} onDelete={noop} />
      </ul>,
    );

    expect(screen.getByText("Urgent")).toBeInTheDocument();
    expect(screen.getByText("Work")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("checkbox", { name: "Complete Write report" }));
    expect(onToggle).toHaveBeenCalledWith(task);

    await userEvent.click(screen.getByRole("button", { name: "Edit Write report" }));
    expect(onEdit).toHaveBeenCalledWith(task);
  });

  it("expands to show the description when the row is clicked, and collapses again", async () => {
    const onEdit = vi.fn();
    const task = makeTask({ id: "1", title: "Write report", priority: "URGENT", description: "Q3 numbers\nand charts" });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={onEdit} onDelete={noop} /></ul>);
    expect(screen.queryByTestId("task-details")).not.toBeInTheDocument();

    await userEvent.click(screen.getByText("Urgent")); // the badge, not the title

    expect(screen.getByTestId("task-details")).toHaveTextContent("Q3 numbers and charts");
    expect(screen.getByRole("button", { name: "Write report" })).toHaveAttribute("aria-expanded", "true");

    await userEvent.click(screen.getByRole("button", { name: "Write report" })); // the title works too
    expect(screen.queryByTestId("task-details")).not.toBeInTheDocument();
    expect(onEdit).not.toHaveBeenCalled(); // expanding never opens the editor
  });

  it("says so when a task has no description", async () => {
    const task = makeTask({ id: "1", title: "Write report", description: null });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={noop} onDelete={noop} /></ul>);

    await userEvent.click(screen.getByRole("button", { name: "Write report" }));

    expect(screen.getByText("No description.")).toBeInTheDocument();
  });

  it("delete and checkbox act on their own without expanding or opening the editor", async () => {
    const onEdit = vi.fn();
    const onDelete = vi.fn();
    const onToggle = vi.fn();
    const task = makeTask({ id: "1", title: "Write report" });
    render(<ul><TaskItem task={task} onToggle={onToggle} onEdit={onEdit} onDelete={onDelete} /></ul>);

    await userEvent.click(screen.getByRole("button", { name: "Delete Write report" }));
    await userEvent.click(screen.getByRole("checkbox"));

    expect(onDelete).toHaveBeenCalledWith(task);
    expect(onToggle).toHaveBeenCalledWith(task);
    expect(onEdit).not.toHaveBeenCalled();
    expect(screen.queryByTestId("task-details")).not.toBeInTheDocument();
  });

  it("marks a finished task as Done", () => {
    const task = makeTask({ id: "1", title: "Write report", status: "DONE" });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={noop} onDelete={noop} /></ul>);

    expect(screen.getByText("Done")).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Reopen Write report" })).toBeChecked();
  });
});
