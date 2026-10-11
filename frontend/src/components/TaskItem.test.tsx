import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { makeCategory, makeTask } from "../test/fakeApi";
import { TaskItem } from "./TaskItem";

const noop = () => {};

describe("TaskItem", () => {
  it("highlights an open task whose due date has passed", () => {
    const task = makeTask({ id: "1", title: "Pay rent", dueDate: "2000-01-01" });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={noop} /></ul>);

    expect(screen.getByRole("listitem")).toHaveAttribute("data-overdue", "true");
    expect(screen.getByText(/Overdue · 2000-01-01/)).toBeInTheDocument();
  });

  it("does not highlight a finished task even if the date passed", () => {
    const task = makeTask({ id: "1", title: "Pay rent", dueDate: "2000-01-01", status: "DONE" });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={noop} /></ul>);

    expect(screen.getByRole("listitem")).not.toHaveAttribute("data-overdue");
    expect(screen.getByRole("checkbox")).toBeChecked();
  });

  it("does not highlight a future due date", () => {
    const task = makeTask({ id: "1", title: "Later", dueDate: "2999-01-01" });
    render(<ul><TaskItem task={task} onToggle={noop} onEdit={noop} /></ul>);

    expect(screen.getByRole("listitem")).not.toHaveAttribute("data-overdue");
    expect(screen.getByText("Due 2999-01-01")).toBeInTheDocument();
  });

  it("shows priority and category and reports toggle and edit", async () => {
    const onToggle = vi.fn();
    const onEdit = vi.fn();
    const task = makeTask({ id: "1", title: "Write report", priority: "URGENT", categoryId: "c1" });
    render(
      <ul>
        <TaskItem task={task} category={makeCategory({ id: "c1", name: "Work" })} onToggle={onToggle} onEdit={onEdit} />
      </ul>,
    );

    expect(screen.getByText("Urgent")).toBeInTheDocument();
    expect(screen.getByText("Work")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("checkbox", { name: "Complete Write report" }));
    expect(onToggle).toHaveBeenCalledWith(task);

    await userEvent.click(screen.getByRole("button", { name: "Write report" }));
    expect(onEdit).toHaveBeenCalledWith(task);
  });
});
