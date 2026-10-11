package com.ihelper.task;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskPurgeRequest(@NotEmpty @Size(max = 500) List<@NotNull UUID> ids) {
}
