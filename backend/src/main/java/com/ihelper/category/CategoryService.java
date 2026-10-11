package com.ihelper.category;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ihelper.common.BadRequestException;
import com.ihelper.common.ConflictException;
import com.ihelper.common.Created;
import com.ihelper.common.CurrentUser;
import com.ihelper.common.NotFoundException;
import com.ihelper.task.TaskRepository;

@Service
@Transactional
public class CategoryService {

	private final CategoryRepository categories;

	private final TaskRepository tasks;

	private final Clock clock;

	private final CurrentUser currentUser;

	public CategoryService(CategoryRepository categories, TaskRepository tasks, Clock clock,
			CurrentUser currentUser) {
		this.categories = categories;
		this.tasks = tasks;
		this.clock = clock;
		this.currentUser = currentUser;
	}

	@Transactional(readOnly = true)
	public List<CategoryDto> list() {
		return categories.findAllByOwnerIdAndDeletedAtIsNullOrderByPositionAscNameAsc(currentUser.id()).stream().map(CategoryDto::from).toList();
	}

	@Transactional(readOnly = true)
	public CategoryDto get(UUID id) {
		return CategoryDto.from(find(id));
	}

	public Created<CategoryDto> create(CategoryCreateRequest request) {
		UUID id = request.id() != null ? request.id() : UUID.randomUUID();
		UUID owner = currentUser.id();
		var existing = categories.findById(id);
		if (existing.isPresent()) {
			// An id owned by another user must look like any other taken id: no data, no hint of the owner.
			if (!owner.equals(existing.get().getOwnerId())) {
				throw new ConflictException("Id " + id + " is already in use");
			}
			return new Created<>(CategoryDto.from(existing.get()), false);
		}
		String name = request.name().trim();
		if (categories.existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNull(owner, name)) {
			throw new ConflictException("A category named '" + name + "' already exists");
		}
		Category category = new Category();
		category.setId(id);
		category.setOwnerId(owner);
		category.setName(name);
		category.setColor(request.color());
		category.setPosition(categories.maxPosition(owner) + 1);
		return new Created<>(CategoryDto.from(categories.saveAndFlush(category)), true);
	}

	public CategoryDto update(UUID id, CategoryUpdateRequest request, Long expectedVersion) {
		Category category = find(id);
		checkVersion(category, expectedVersion);
		String name = request.name().trim();
		if (categories.existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(currentUser.id(), name, id)) {
			throw new ConflictException("A category named '" + name + "' already exists");
		}
		category.setName(name);
		category.setColor(request.color());
		return CategoryDto.from(categories.saveAndFlush(category));
	}

	/**
	 * Puts the given categories first, in the given order; live categories that are not listed keep their
	 * relative order after them, so a client with a slightly stale list cannot lose anything.
	 * Only categories whose position actually changes are written (and so bump their version).
	 */
	public List<CategoryDto> reorder(List<UUID> orderedIds) {
		Map<UUID, Category> live = new LinkedHashMap<>();
		for (Category c : categories.findAllByOwnerIdAndDeletedAtIsNullOrderByPositionAscNameAsc(currentUser.id())) {
			live.put(c.getId(), c);
		}
		var seen = new HashSet<UUID>();
		List<Category> ordered = new ArrayList<>();
		for (UUID id : orderedIds) {
			if (!seen.add(id)) {
				throw new BadRequestException("Category id " + id + " is listed more than once");
			}
			Category c = live.get(id);
			if (c == null) {
				throw new BadRequestException("Category id " + id + " does not refer to an existing category");
			}
			ordered.add(c);
		}
		live.values().stream().filter(c -> !seen.contains(c.getId())).forEach(ordered::add);
		for (int i = 0; i < ordered.size(); i++) {
			Category c = ordered.get(i);
			if (c.getPosition() != i) {
				c.setPosition(i);
				categories.save(c);
			}
		}
		categories.flush();
		return ordered.stream().map(CategoryDto::from).toList();
	}

	/** Soft-deletes the category and moves its tasks to Inbox. Deleting twice is a no-op. */
	public void delete(UUID id, Long expectedVersion) {
		Category category = categories.findByIdAndOwnerId(id, currentUser.id())
			.orElseThrow(() -> new NotFoundException("Category", id));
		if (category.isDeleted()) {
			return;
		}
		checkVersion(category, expectedVersion);
		Instant now = clock.instant();
		tasks.moveToInbox(currentUser.id(), id, now);
		category.setDeletedAt(now);
		categories.saveAndFlush(category);
	}

	private Category find(UUID id) {
		return categories.findByIdAndOwnerIdAndDeletedAtIsNull(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Category", id));
	}

	private void checkVersion(Category category, Long expected) {
		if (expected != null && !expected.equals(category.getVersion())) {
			throw new ConflictException(
					"Category was modified (expected version " + expected + ", current " + category.getVersion() + ")",
					CategoryDto.from(category));
		}
	}

}
