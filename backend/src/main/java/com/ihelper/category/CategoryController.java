package com.ihelper.category;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ihelper.common.Created;
import com.ihelper.common.Versions;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories")
public class CategoryController {

	private final CategoryService service;

	public CategoryController(CategoryService service) {
		this.service = service;
	}

	@GetMapping
	@Operation(summary = "List all live categories (a small bounded set, so not paginated)")
	public List<CategoryDto> list() {
		return service.list();
	}

	@GetMapping("/{id}")
	public CategoryDto get(@PathVariable UUID id) {
		return service.get(id);
	}

	@PostMapping
	@Operation(summary = "Create a category",
			description = "201 when created. If the client-supplied id already exists, 200 with the existing category.")
	public ResponseEntity<CategoryDto> create(@Valid @RequestBody CategoryCreateRequest request) {
		Created<CategoryDto> result = service.create(request);
		if (!result.created()) {
			return ResponseEntity.ok(result.value());
		}
		return ResponseEntity.created(URI.create("/api/v1/categories/" + result.value().id())).body(result.value());
	}

	@PutMapping("/{id}")
	@Operation(summary = "Replace a category; 409 with the current copy if the version is stale")
	public CategoryDto update(@PathVariable UUID id, @Valid @RequestBody CategoryUpdateRequest request,
			@RequestHeader(value = "If-Match", required = false) String ifMatch) {
		return service.update(id, request, Versions.require(request.version(), ifMatch));
	}

	@PutMapping("/order")
	@Operation(summary = "Set the display order of categories",
			description = "Listed categories come first in the given order; unlisted ones follow in their current order. "
					+ "Returns every live category in the new order.")
	public List<CategoryDto> reorder(@Valid @RequestBody CategoryReorderRequest request) {
		return service.reorder(request.ids());
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Soft-delete a category; its tasks move to Inbox",
			description = "Idempotent: deleting an already deleted category returns 204. An optional If-Match version guards against stale deletes.")
	public ResponseEntity<Void> delete(@PathVariable UUID id,
			@RequestHeader(value = "If-Match", required = false) String ifMatch) {
		service.delete(id, Versions.resolve(null, ifMatch));
		return ResponseEntity.noContent().build();
	}

}
