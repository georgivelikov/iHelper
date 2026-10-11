package com.ihelper.sync;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ihelper.category.CategoryDto;
import com.ihelper.category.CategoryRepository;
import com.ihelper.common.CurrentUser;
import com.ihelper.task.TaskDto;
import com.ihelper.task.TaskRepository;

@Service
@Transactional(readOnly = true)
public class SyncService {

	private final CategoryRepository categories;

	private final TaskRepository tasks;

	private final Clock clock;

	private final CurrentUser currentUser;

	public SyncService(CategoryRepository categories, TaskRepository tasks, Clock clock,
			CurrentUser currentUser) {
		this.categories = categories;
		this.tasks = tasks;
		this.clock = clock;
		this.currentUser = currentUser;
	}

	/**
	 * With no {@code since} this is a first full sync, so rows that are already deleted are left out:
	 * a fresh client has nothing to remove.
	 */
	public SyncResponse changesSince(Instant since) {
		// Read the clock first: a row written while we query is then reported again next time, never missed.
		Instant serverTime = clock.instant();
		boolean initial = since == null;
		Instant from = initial ? Instant.EPOCH : since;
		var changedCategories = categories.findByOwnerIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(currentUser.id(), from).stream()
			.filter(c -> !initial || !c.isDeleted())
			.map(CategoryDto::from)
			.toList();
		var changedTasks = tasks.findByOwnerIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(currentUser.id(), from).stream()
			.filter(t -> !initial || !t.isDeleted())
			.map(TaskDto::from)
			.toList();
		return new SyncResponse(serverTime, changedCategories, changedTasks);
	}

}
