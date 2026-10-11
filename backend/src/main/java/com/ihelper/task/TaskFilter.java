package com.ihelper.task;

import java.util.Set;
import java.util.UUID;

/** {@code statuses} null/empty means any status. {@code inbox} selects tasks without a category and wins over {@code categoryId}. */
public record TaskFilter(UUID categoryId, boolean inbox, Priority priority, Set<TaskStatus> statuses, String q) {
}
