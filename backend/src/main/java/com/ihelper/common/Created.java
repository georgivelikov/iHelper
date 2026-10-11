package com.ihelper.common;

/** Result of an idempotent create: {@code created} is false when the client's id already existed. */
public record Created<T>(T value, boolean created) {
}
