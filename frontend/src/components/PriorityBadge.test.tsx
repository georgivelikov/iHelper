import { render, screen } from "@testing-library/react";
import { PriorityBadge } from "./PriorityBadge";

describe("PriorityBadge", () => {
  it.each([
    ["URGENT", "Urgent"],
    ["HIGH", "High"],
    ["MEDIUM", "Medium"],
    ["LOW", "Low"],
  ] as const)("shows %s as %s", (priority, label) => {
    render(<PriorityBadge priority={priority} />);
    const badge = screen.getByText(label);
    expect(badge).toHaveAttribute("data-priority", priority);
  });
});
