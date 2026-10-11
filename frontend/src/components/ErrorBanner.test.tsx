import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiError } from "../api/errors";
import { ErrorBanner } from "./ErrorBanner";

describe("ErrorBanner", () => {
  it("renders nothing without an error", () => {
    const { container } = render(<ErrorBanner error={null} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows problem title, detail and field-level errors", () => {
    const error = new ApiError(400, {
      title: "Validation failed",
      detail: "Request is invalid",
      errors: [{ field: "title", message: "must not be blank" }],
    });
    render(<ErrorBanner error={error} />);

    expect(screen.getByRole("alert")).toHaveTextContent("Validation failed: Request is invalid");
    expect(screen.getByText("title: must not be blank")).toBeInTheDocument();
  });

  it("explains a version conflict in plain words", () => {
    render(<ErrorBanner error={new ApiError(409, { title: "Conflict" })} />);
    expect(screen.getByRole("alert")).toHaveTextContent(/changed elsewhere/i);
  });

  it("falls back to the message of ordinary errors and can be dismissed", async () => {
    const onDismiss = vi.fn();
    render(<ErrorBanner error={new TypeError("Failed to fetch")} onDismiss={onDismiss} />);

    expect(screen.getByRole("alert")).toHaveTextContent("Failed to fetch");
    await userEvent.click(screen.getByRole("button", { name: "Dismiss error" }));
    expect(onDismiss).toHaveBeenCalled();
  });
});
