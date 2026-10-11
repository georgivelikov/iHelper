package com.ihelper.common;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/** Page numbers are zero-based. */
@Schema(requiredProperties = { "items", "page", "size", "total" })
public record PageResponse<T>(List<T> items, int page, int size, long total) {

	public static <T> PageResponse<T> of(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
	}

}
