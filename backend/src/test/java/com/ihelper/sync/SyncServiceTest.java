package com.ihelper.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihelper.category.CategoryRepository;
import com.ihelper.task.Task;
import com.ihelper.task.TaskRepository;

@ExtendWith(MockitoExtension.class)
class SyncServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

	@Mock
	CategoryRepository categories;

	@Mock
	TaskRepository tasks;

	private static Task task(boolean deleted) {
		Task t = new Task();
		t.setId(UUID.randomUUID());
		t.setTitle("t");
		if (deleted) {
			t.setDeletedAt(NOW);
		}
		return t;
	}

	@Test
	void incrementalSyncIncludesDeletedRowsAndReturnsServerTime() {
		Instant since = NOW.minusSeconds(3600);
		when(categories.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(since)).thenReturn(List.of());
		when(tasks.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(since)).thenReturn(List.of(task(false), task(true)));
		var service = new SyncService(categories, tasks, Clock.fixed(NOW, ZoneOffset.UTC));

		var response = service.changesSince(since);

		assertThat(response.serverTime()).isEqualTo(NOW);
		assertThat(response.tasks()).extracting(t -> t.deletedAt() != null).containsExactly(false, true);
	}

	@Test
	void firstFullSyncOmitsDeletedRows() {
		when(categories.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Instant.EPOCH)).thenReturn(List.of());
		when(tasks.findByUpdatedAtGreaterThanOrderByUpdatedAtAsc(Instant.EPOCH))
			.thenReturn(List.of(task(false), task(true)));
		var service = new SyncService(categories, tasks, Clock.fixed(NOW, ZoneOffset.UTC));

		assertThat(service.changesSince(null).tasks()).hasSize(1);
	}

}
