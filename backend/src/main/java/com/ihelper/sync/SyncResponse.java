package com.ihelper.sync;

import java.time.Instant;
import java.util.List;

import com.ihelper.category.CategoryDto;
import com.ihelper.task.TaskDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Everything that changed after {@code since}, soft-deleted rows included (their {@code deletedAt} is set).
 * Clients store {@code serverTime} and send it as {@code since} next time.
 */
@Schema(requiredProperties = { "serverTime", "categories", "tasks" })
public record SyncResponse(Instant serverTime, List<CategoryDto> categories, List<TaskDto> tasks) {
}
