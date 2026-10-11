import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { makeCategory } from "../test/fakeApi";
import { CategorySidebar } from "./CategorySidebar";

const categories = [
  makeCategory({ id: "c1", name: "Personal", position: 0 }),
  makeCategory({ id: "c2", name: "Ideas", position: 1 }),
  makeCategory({ id: "c3", name: "Work", position: 2 }),
];

function renderSidebar(onReorder = vi.fn()) {
  render(
    <CategorySidebar
      categories={categories}
      counts={{ total: 5, inbox: 1, byCategory: {}, bin: 2 }}
      selected="all"
      onSelect={vi.fn()}
      onCreate={vi.fn()}
      onUpdate={vi.fn()}
      onDelete={vi.fn()}
      onReorder={onReorder}
    />,
  );
  return onReorder;
}

// jsdom has no layout, so every box is 0x0 and the drag has nowhere to go. Give each list item a
// slot in a vertical stack so the keyboard sensor can move the dragged row past its neighbour.
const original = HTMLElement.prototype.getBoundingClientRect;
beforeEach(() => {
  HTMLElement.prototype.getBoundingClientRect = function (this: HTMLElement) {
    const items = Array.from(this.closest("ul")?.children ?? []);
    const li = this.closest("li");
    const index = li ? items.indexOf(li) : 0;
    return { x: 0, y: index * 40, top: index * 40, left: 0, bottom: index * 40 + 40, right: 200, width: 200, height: 40, toJSON() {} };
  };
});
afterEach(() => {
  HTMLElement.prototype.getBoundingClientRect = original;
});

describe("CategorySidebar", () => {
  it("labels the all-tasks entry Home and shows the bin with its count", () => {
    renderSidebar();

    expect(screen.getByRole("button", { name: /^Home\s*5/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Bin\s*2/ })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /^All/ })).not.toBeInTheDocument();
  });

  it("gives every category a drag handle", () => {
    renderSidebar();

    for (const name of ["Personal", "Ideas", "Work"]) {
      expect(screen.getByRole("button", { name: `Reorder category ${name}` })).toBeInTheDocument();
    }
  });

  it("reports the new order after a keyboard drag", async () => {
    const onReorder = renderSidebar();

    const handle = screen.getByRole("button", { name: "Reorder category Personal" });
    handle.focus();
    await userEvent.keyboard(" ");
    await userEvent.keyboard("{ArrowDown}");
    await userEvent.keyboard(" ");

    expect(onReorder).toHaveBeenCalledTimes(1);
    expect(onReorder).toHaveBeenCalledWith(["c2", "c1", "c3"]);
  });
});
