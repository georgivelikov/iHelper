package com.ihelper.sync;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Sync")
public class SyncController {

	private final SyncService service;

	public SyncController(SyncService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "Categories and tasks changed after a point in time, including deletions")
	public SyncResponse sync(
			@Parameter(description = "UTC ISO-8601 instant, normally the serverTime of the previous sync. "
					+ "Omit for a full first sync.")
			@RequestParam(required = false) Instant since) {
		return service.changesSince(since);
	}

}
