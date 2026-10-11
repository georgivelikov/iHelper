import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Filters } from "./Filters";

function setup() {
  const handlers = { onStatus: vi.fn(), onPriority: vi.fn(), onSort: vi.fn(), onSearch: vi.fn() };
  render(<Filters status="open" priority="" sort="priority" q="" {...handlers} />);
  return handlers;
}

describe("Filters", () => {
  it("reports status, priority and sort changes", async () => {
    const h = setup();

    await userEvent.selectOptions(screen.getByLabelText("Show"), "done");
    await userEvent.selectOptions(screen.getByLabelText("Priority"), "URGENT");
    await userEvent.selectOptions(screen.getByLabelText("Sort by"), "due");

    expect(h.onStatus).toHaveBeenCalledWith("done");
    expect(h.onPriority).toHaveBeenCalledWith("URGENT");
    expect(h.onSort).toHaveBeenCalledWith("due");
  });

  it("reports search text", async () => {
    const h = setup();

    await userEvent.type(screen.getByLabelText("Search tasks"), "a");

    expect(h.onSearch).toHaveBeenCalledWith("a");
  });
});
