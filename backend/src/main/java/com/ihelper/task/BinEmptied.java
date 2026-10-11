package com.ihelper.task;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = { "purged" })
public record BinEmptied(int purged) {
}
