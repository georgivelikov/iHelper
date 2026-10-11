package com.ihelper.task;

import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ihelper.common.BadRequestException;
import com.ihelper.common.Created;
import com.ihelper.common.PageResponse;
import com.ihelper.common.Versions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks")
public class TaskController {

	private final TaskService service;

	private final ObjectMapper mapper;

	public TaskController(TaskService service, ObjectMapper mapper) {
		this.service = service;
		this.mapper = mapper;
	}

	@GetMapping
	@Operation(summary = "List live tasks, filtered, sorted and paginated")
	public PageResponse<TaskDto> list(
			@RequestParam(required = false) UUID categoryId,
			@Parameter(description = "true = only tasks without a category (Inbox); overrides categoryId")
			@RequestParam(defaultValue = "false") boolean inbox,
			@RequestParam(required = false) Priority priority,
			@Parameter(description = "Repeatable: status=TODO&status=IN_PROGRESS") @RequestParam(required = false) Set<TaskStatus> status,
			@Parameter(description = "Case-insensitive search in title and description") @RequestParam(required = false) String q,
			@Parameter(description = "Comma separated fields, '-' prefix = descending, e.g. priority,-dueDate. "
					+ "Fields: priority, dueDate, createdAt, updatedAt, title, status. Default: -priority,dueDate,createdAt")
			@RequestParam(required = false) String sort,
			@Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		var filter = new TaskFilter(categoryId, inbox, priority, status, q);
		return PageResponse.of(service.list(filter, sort, page, size));
	}

	@GetMapping("/bin")
	@Operation(summary = "List the tasks in the bin, most recently deleted first")
	public PageResponse<TaskDto> bin(
			@Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size) {
		return PageResponse.of(service.bin(page, size));
	}

	@DeleteMapping("/bin")
	@Operation(summary = "Empty the bin: permanently erase every task in it",
			description = "The task content is wiped; only an id and a deleted marker stay so other devices learn about it through /sync. Cannot be undone.")
	public BinEmptied emptyBin() {
		return new BinEmptied(service.emptyBin());
	}

	@GetMapping("/counts")
	@Operation(summary = "Number of open (not DONE) tasks in total, in Inbox and per category")
	public TaskCounts counts() {
		return service.counts();
	}

	@GetMapping("/{id}")
	public TaskDto get(@PathVariable UUID id) {
		return service.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a task",
			description = "201 when created. If the client-supplied id already exists, 200 with the existing task.")
	public ResponseEntity<TaskDto> create(@Valid @RequestBody TaskCreateRequest request) {
		Created<TaskDto> result = service.create(request);
		if (!result.created()) {
			return ResponseEntity.ok(result.value());
		}
		return ResponseEntity.created(URI.create("/api/v1/tasks/" + result.value().id())).body(result.value());
	}

	@PutMapping("/{id}")
	@Operation(summary = "Replace a task; 409 with the current copy if the version is stale")
	public TaskDto update(@PathVariable UUID id, @Valid @RequestBody TaskUpdateRequest request,
			@RequestHeader(value = "If-Match", required = false) String ifMatch) {
		return service.update(id, request, Versions.require(request.version(), ifMatch));
	}

	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Partially update a task, e.g. {\"status\":\"DONE\"}",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					content = @Content(schema = @Schema(implementation = TaskPatchRequest.class))))
	public TaskDto patch(@PathVariable UUID id, @RequestBody JsonNode body,
			@RequestHeader(value = "If-Match", required = false) String ifMatch) {
		if (!body.isObject()) {
			throw new BadRequestException("PATCH body must be a JSON object");
		}
		Set<String> present = new LinkedHashSet<>();
		for (String field : TaskPatchRequest.FIELDS) {
			if (body.has(field)) {
				present.add(field);
			}
		}
		TaskPatchRequest values = mapper.treeToValue(body, TaskPatchRequest.class);
		return service.patch(id, new TaskPatch(present, values), Versions.resolve(values.version(), ifMatch));
	}

	@PostMapping("/{id}/restore")
	@Operation(summary = "Take a task out of the bin",
			description = "Idempotent for a task that is not in the bin. 404 once the bin has been emptied. An optional If-Match version guards against stale restores.")
	public TaskDto restore(@PathVariable UUID id,
			@RequestHeader(value = "If-Match", required = false) String ifMatch) {
		return service.restore(id, Versions.resolve(null, ifMatch));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Move a task to the bin (soft delete)",
			description = "Idempotent: deleting an already deleted task returns 204. An optional If-Match version guards against stale deletes.")
	public ResponseEntity<Void> delete(@PathVariable UUID id,
			@RequestHeader(value = "If-Match", required = false) String ifMatch) {
		service.delete(id, Versions.resolve(null, ifMatch));
		return ResponseEntity.noContent().build();
	}

}
