package com.ihelper.category;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryReorderRequest(@NotNull @Size(max = 500) List<@NotNull UUID> ids) {
}
