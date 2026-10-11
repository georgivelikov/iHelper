package com.ihelper.task;

import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Open (not DONE) task counts for the category sidebar. {@code total} includes Inbox;
 * {@code bin} is the number of tasks waiting in the bin.
 */
@Schema(requiredProperties = { "total", "inbox", "byCategory", "bin" })
public record TaskCounts(long total, long inbox, Map<UUID, Long> byCategory, long bin) {
}
