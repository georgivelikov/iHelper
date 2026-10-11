package com.ihelper.task;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ihelper.category.CategoryRepository;
import com.ihelper.common.BadRequestException;
import com.ihelper.common.ConflictException;
import com.ihelper.common.Created;
import com.ihelper.common.CurrentUser;
import com.ihelper.common.NotFoundException;

@Service
@Transactional
public class TaskService {

	static final int MAX_PAGE_SIZE = 200;

	private static final Set<String> SORTABLE = Set.of("priority", "dueDate", "createdAt", "updatedAt", "title",
			"status");

	private final TaskRepository tasks;

	private final CategoryRepository categories;

	private final Clock clock;

	private final CurrentUser currentUser;

	public TaskService(TaskRepository tasks, CategoryRepository categories, Clock clock,
			CurrentUser currentUser) {
		this.tasks = tasks;
		this.categories = categories;
		this.clock = clock;
		this.currentUser = currentUser;
	}

	@Transactional(readOnly = true)
	public Page<TaskDto> list(TaskFilter filter, String sort, int page, int size) {
		if (page < 0) {
			throw new BadRequestException("page must be >= 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		PageRequest pageable = PageRequest.of(page, size, parseSort(sort));
		return tasks.findAll(TaskSpecifications.matching(currentUser.id(), filter), pageable).map(TaskDto::from);
	}

	/** The bin, most recently deleted first. */
	@Transactional(readOnly = true)
	public Page<TaskDto> bin(int page, int size) {
		if (page < 0) {
			throw new BadRequestException("page must be >= 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("deletedAt"), Sort.Order.asc("id")));
		return tasks.findByOwnerIdAndDeletedAtIsNotNullAndPurgedAtIsNull(currentUser.id(), pageable).map(TaskDto::from);
	}

	@Transactional(readOnly = true)
	public TaskCounts counts() {
		long inbox = 0;
		long total = 0;
		Map<UUID, Long> byCategory = new LinkedHashMap<>();
		for (var row : tasks.countOpenByCategory(currentUser.id())) {
			total += row.getOpen();
			if (row.getCategoryId() == null) {
				inbox = row.getOpen();
			}
			else {
				byCategory.put(row.getCategoryId(), row.getOpen());
			}
		}
		return new TaskCounts(total, inbox, byCategory,
				tasks.countByOwnerIdAndDeletedAtIsNotNullAndPurgedAtIsNull(currentUser.id()));
	}

	@Transactional(readOnly = true)
	public TaskDto get(UUID id) {
		return TaskDto.from(find(id));
	}

	public Created<TaskDto> create(TaskCreateRequest request) {
		UUID id = request.id() != null ? request.id() : UUID.randomUUID();
		UUID owner = currentUser.id();
		var existing = tasks.findById(id);
		if (existing.isPresent()) {
			// An id owned by another user must look like any other taken id: no data, no hint of the owner.
			if (!owner.equals(existing.get().getOwnerId())) {
				throw new ConflictException("Id " + id + " is already in use");
			}
			return new Created<>(TaskDto.from(existing.get()), false);
		}
		requireCategory(request.categoryId());
		Task task = new Task();
		task.setId(id);
		task.setOwnerId(owner);
		task.setTitle(request.title().trim());
		task.setDescription(request.description());
		task.setCategoryId(request.categoryId());
		task.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
		task.setDueDate(request.dueDate());
		applyStatus(task, request.status() != null ? request.status() : TaskStatus.TODO);
		return new Created<>(TaskDto.from(tasks.saveAndFlush(task)), true);
	}

	public TaskDto update(UUID id, TaskUpdateRequest request, Long expectedVersion) {
		Task task = find(id);
		checkVersion(task, expectedVersion);
		requireCategory(request.categoryId());
		task.setTitle(request.title().trim());
		task.setDescription(request.description());
		task.setCategoryId(request.categoryId());
		task.setPriority(request.priority());
		task.setDueDate(request.dueDate());
		applyStatus(task, request.status());
		return TaskDto.from(tasks.saveAndFlush(task));
	}

	public TaskDto patch(UUID id, TaskPatch patch, Long expectedVersion) {
		Task task = find(id);
		checkVersion(task, expectedVersion);
		TaskPatchRequest v = patch.values();
		if (patch.has("title")) {
			if (v.title() == null || v.title().isBlank() || v.title().length() > 200) {
				throw new BadRequestException("title must be 1-200 characters");
			}
			task.setTitle(v.title().trim());
		}
		if (patch.has("description")) {
			if (v.description() != null && v.description().length() > 5000) {
				throw new BadRequestException("description must be at most 5000 characters");
			}
			task.setDescription(v.description());
		}
		if (patch.has("categoryId")) {
			requireCategory(v.categoryId());
			task.setCategoryId(v.categoryId());
		}
		if (patch.has("priority")) {
			if (v.priority() == null) {
				throw new BadRequestException("priority cannot be null");
			}
			task.setPriority(v.priority());
		}
		if (patch.has("status")) {
			if (v.status() == null) {
				throw new BadRequestException("status cannot be null");
			}
			applyStatus(task, v.status());
		}
		if (patch.has("dueDate")) {
			task.setDueDate(v.dueDate());
		}
		return TaskDto.from(tasks.saveAndFlush(task));
	}

	/** Soft delete. Deleting an already deleted task is a no-op so offline retries are safe. */
	public void delete(UUID id, Long expectedVersion) {
		Task task = tasks.findByIdAndOwnerId(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Task", id));
		if (task.isDeleted()) {
			return;
		}
		checkVersion(task, expectedVersion);
		task.setDeletedAt(clock.instant());
		tasks.saveAndFlush(task);
	}

	/**
	 * Takes a task out of the bin. Restoring a task that is not in the bin is a no-op; an emptied
	 * (purged) task cannot come back. A task whose category was deleted meanwhile returns to Inbox.
	 */
	public TaskDto restore(UUID id, Long expectedVersion) {
		Task task = tasks.findByIdAndOwnerId(id, currentUser.id())
			.filter(t -> t.getPurgedAt() == null)
			.orElseThrow(() -> new NotFoundException("Task", id));
		if (!task.isDeleted()) {
			return TaskDto.from(task);
		}
		checkVersion(task, expectedVersion);
		task.setDeletedAt(null);
		if (task.getCategoryId() != null
				&& categories.findByIdAndOwnerIdAndDeletedAtIsNull(task.getCategoryId(), currentUser.id()).isEmpty()) {
			task.setCategoryId(null);
		}
		return TaskDto.from(tasks.saveAndFlush(task));
	}

	/** Final deletion of everything in the bin. Returns how many tasks were erased. */
	public int emptyBin() {
		return tasks.purgeBin(currentUser.id(), clock.instant());
	}

	private Task find(UUID id) {
		return tasks.findByIdAndOwnerIdAndDeletedAtIsNull(id, currentUser.id()).orElseThrow(() -> new NotFoundException("Task", id));
	}

	private void checkVersion(Task task, Long expected) {
		if (expected != null && !expected.equals(task.getVersion())) {
			throw new ConflictException(
					"Task was modified (expected version " + expected + ", current " + task.getVersion() + ")",
					TaskDto.from(task));
		}
	}

	private void requireCategory(UUID categoryId) {
		if (categoryId != null && categories.findByIdAndOwnerIdAndDeletedAtIsNull(categoryId, currentUser.id()).isEmpty()) {
			throw new BadRequestException("categoryId " + categoryId + " does not refer to an existing category");
		}
	}

	/** DONE stamps {@code completedAt} once; any other status clears it. */
	private void applyStatus(Task task, TaskStatus status) {
		if (status == TaskStatus.DONE) {
			if (task.getStatus() != TaskStatus.DONE || task.getCompletedAt() == null) {
				task.setCompletedAt(clock.instant());
			}
		}
		else {
			task.setCompletedAt(null);
		}
		task.setStatus(status);
	}

	/** {@code priority,-dueDate}: comma separated, a leading '-' means descending. */
	static Sort parseSort(String sort) {
		List<Sort.Order> orders = new ArrayList<>();
		if (sort == null || sort.isBlank()) {
			orders.add(Sort.Order.desc("priority"));
			orders.add(Sort.Order.asc("dueDate").nullsLast());
			orders.add(Sort.Order.asc("createdAt"));
		}
		else {
			for (String part : sort.split(",")) {
				String token = part.trim();
				boolean desc = token.startsWith("-");
				String field = desc ? token.substring(1) : token;
				if (!SORTABLE.contains(field)) {
					throw new BadRequestException("Cannot sort by '" + field + "'; allowed: " + new java.util.TreeSet<>(SORTABLE));
				}
				Sort.Order order = desc ? Sort.Order.desc(field) : Sort.Order.asc(field);
				orders.add(field.equals("dueDate") ? order.nullsLast() : order);
			}
		}
		orders.add(Sort.Order.asc("id")); // stable paging
		return Sort.by(orders);
	}

}
