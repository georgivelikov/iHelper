package com.ihelper.sync;

import java.time.Instant;
import java.util.List;

import com.ihelper.category.CategoryDto;
import com.ihelper.task.TaskDto;

/**
 * Everything that changed after {@code since}, soft-deleted rows included (their {@code deletedAt} is set).
 * Clients store {@code serverTime} and send it as {@code since} next time.
 */
public record SyncResponse(Instant serverTime, List<CategoryDto> categories, List<TaskDto> tasks) {
}
