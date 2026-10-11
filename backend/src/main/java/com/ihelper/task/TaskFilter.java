package com.ihelper.task;

import java.util.UUID;

/** {@code inbox} selects tasks without a category and wins over {@code categoryId}. */
public record TaskFilter(UUID categoryId, boolean inbox, Priority priority, TaskStatus status, String q) {
}
