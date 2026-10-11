package com.ihelper.category;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ihelper.common.ConflictException;
import com.ihelper.common.Created;
import com.ihelper.common.NotFoundException;
import com.ihelper.task.TaskRepository;

@Service
@Transactional
public class CategoryService {

	private final CategoryRepository categories;

	private final TaskRepository tasks;

	private final Clock clock;

	public CategoryService(CategoryRepository categories, TaskRepository tasks, Clock clock) {
		this.categories = categories;
		this.tasks = tasks;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public List<CategoryDto> list() {
		return categories.findAllByDeletedAtIsNullOrderByNameAsc().stream().map(CategoryDto::from).toList();
	}

	@Transactional(readOnly = true)
	public CategoryDto get(UUID id) {
		return CategoryDto.from(find(id));
	}

	public Created<CategoryDto> create(CategoryCreateRequest request) {
		UUID id = request.id() != null ? request.id() : UUID.randomUUID();
		var existing = categories.findById(id);
		if (existing.isPresent()) {
			return new Created<>(CategoryDto.from(existing.get()), false);
		}
		String name = request.name().trim();
		if (categories.existsByNameIgnoreCaseAndDeletedAtIsNull(name)) {
			throw new ConflictException("A category named '" + name + "' already exists");
		}
		Category category = new Category();
		category.setId(id);
		category.setName(name);
		category.setColor(request.color());
		return new Created<>(CategoryDto.from(categories.saveAndFlush(category)), true);
	}

	public CategoryDto update(UUID id, CategoryUpdateRequest request, Long expectedVersion) {
		Category category = find(id);
		checkVersion(category, expectedVersion);
		String name = request.name().trim();
		if (categories.existsByNameIgnoreCaseAndDeletedAtIsNullAndIdNot(name, id)) {
			throw new ConflictException("A category named '" + name + "' already exists");
		}
		category.setName(name);
		category.setColor(request.color());
		return CategoryDto.from(categories.saveAndFlush(category));
	}

	/** Soft-deletes the category and moves its tasks to Inbox. Deleting twice is a no-op. */
	public void delete(UUID id, Long expectedVersion) {
		Category category = categories.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
		if (category.isDeleted()) {
			return;
		}
		checkVersion(category, expectedVersion);
		Instant now = clock.instant();
		tasks.moveToInbox(id, now);
		category.setDeletedAt(now);
		categories.saveAndFlush(category);
	}

	private Category find(UUID id) {
		return categories.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new NotFoundException("Category", id));
	}

	private void checkVersion(Category category, Long expected) {
		if (expected != null && !expected.equals(category.getVersion())) {
			throw new ConflictException(
					"Category was modified (expected version " + expected + ", current " + category.getVersion() + ")",
					CategoryDto.from(category));
		}
	}

}
