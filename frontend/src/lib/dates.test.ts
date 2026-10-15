import { isDueOrOverdue, isOverdue, todayIso } from "./dates";

describe("todayIso", () => {
  it("formats the local date with zero padding", () => {
    expect(todayIso(new Date(2026, 0, 5, 23, 59))).toBe("2026-01-05");
  });
});

describe("isOverdue", () => {
  const today = "2026-03-10";

  it("is true for an open task due before today", () => {
    expect(isOverdue("2026-03-09", "TODO", today)).toBe(true);
    expect(isOverdue("2026-03-09", "IN_PROGRESS", today)).toBe(true);
  });

  it("glows for today as well as past dates", () => {
    expect(isDueOrOverdue("2026-03-10", "TODO", today)).toBe(true);
    expect(isDueOrOverdue("2026-03-09", "TODO", today)).toBe(true);
    expect(isDueOrOverdue("2026-03-11", "TODO", today)).toBe(false);
    expect(isDueOrOverdue("2026-03-10", "DONE", today)).toBe(false);
  });

  it("is false for today, the future, no date, or a finished task", () => {
    expect(isOverdue("2026-03-10", "TODO", today)).toBe(false);
    expect(isOverdue("2026-04-01", "TODO", today)).toBe(false);
    expect(isOverdue(null, "TODO", today)).toBe(false);
    expect(isOverdue("2020-01-01", "DONE", today)).toBe(false);
  });
});
