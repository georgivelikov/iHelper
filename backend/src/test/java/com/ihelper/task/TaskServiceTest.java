package com.ihelper.task;

import static org.assertj.core.api.Assertions.assertThat;
import static com.ihelper.TestUsers.currentUser;
import static com.ihelper.TestUsers.OWNER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihelper.category.Category;
import com.ihelper.category.CategoryRepository;
import com.ihelper.common.BadRequestException;
import com.ihelper.common.ConflictException;
import com.ihelper.common.NotFoundException;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

	@Mock
	TaskRepository tasks;

	@Mock
	CategoryRepository categories;

	TaskService service;

	@BeforeEach
	void setUp() {
		service = new TaskService(tasks, categories, Clock.fixed(NOW, ZoneOffset.UTC), currentUser());
		Mockito.lenient().when(tasks.saveAndFlush(any(Task.class))).thenAnswer(i -> i.getArgument(0));
	}

	private static Task existing(TaskStatus status) {
		Task t = new Task();
		t.setId(UUID.randomUUID());
		t.setOwnerId(OWNER);
		t.setTitle("existing");
		t.setStatus(status);
		return t;
	}

	private static TaskCreateRequest create(UUID id, UUID categoryId, TaskStatus status) {
		return new TaskCreateRequest(id, "  Buy milk ", null, categoryId, null, status, null);
	}

	private static TaskPatch patchOf(String field, TaskPatchRequest values) {
		return new TaskPatch(Set.of(field), values);
	}

	@Test
	void createAppliesDefaultsAndTrimsTitle() {
		var result = service.create(create(null, null, null));

		assertThat(result.created()).isTrue();
		assertThat(result.value().title()).isEqualTo("Buy milk");
		assertThat(result.value().priority()).isEqualTo(Priority.MEDIUM);
		assertThat(result.value().status()).isEqualTo(TaskStatus.TODO);
		assertThat(result.value().completedAt()).isNull();
	}

	@Test
	void createWithExistingIdIsIdempotent() {
		Task found = existing(TaskStatus.TODO);
		when(tasks.findById(found.getId())).thenReturn(Optional.of(found));

		var result = service.create(create(found.getId(), null, null));

		assertThat(result.created()).isFalse();
		assertThat(result.value().id()).isEqualTo(found.getId());
		verify(tasks, never()).saveAndFlush(any());
	}

	@Test
	void createDoneSetsCompletedAt() {
		var result = service.create(create(null, null, TaskStatus.DONE));

		assertThat(result.value().completedAt()).isEqualTo(NOW);
	}

	@Test
	void createRejectsUnknownCategory() {
		UUID missing = UUID.randomUUID();
		when(categories.findByIdAndOwnerIdAndDeletedAtIsNull(missing, OWNER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(create(null, missing, null)))
			.isInstanceOf(BadRequestException.class);
	}

	@Test
	void createAcceptsExistingCategory() {
		UUID id = UUID.randomUUID();
		when(categories.findByIdAndOwnerIdAndDeletedAtIsNull(id, OWNER)).thenReturn(Optional.of(new Category()));

		assertThat(service.create(create(null, id, null)).value().categoryId()).isEqualTo(id);
	}

	@Test
	void markingDoneStampsCompletedAtAndReopeningClearsIt() {
		Task task = existing(TaskStatus.TODO);
		when(tasks.findByIdAndOwnerIdAndDeletedAtIsNull(task.getId(), OWNER)).thenReturn(Optional.of(task));

		var done = service.patch(task.getId(),
				patchOf("status", new TaskPatchRequest(null, null, null, null, TaskStatus.DONE, null, null)), null);
		assertThat(done.completedAt()).isEqualTo(NOW);

		var reopened = service.patch(task.getId(),
				patchOf("status", new TaskPatchRequest(null, null, null, null, TaskStatus.IN_PROGRESS, null, null)),
				null);
		assertThat(reopened.completedAt()).isNull();
	}

	@Test
	void patchOnlyTouchesPresentFieldsAndExplicitNullClears() {
		Task task = existing(TaskStatus.TODO);
		task.setDescription("keep me");
		task.setDueDate(LocalDate.of(2026, 5, 1));
		task.setPriority(Priority.HIGH);
		when(tasks.findByIdAndOwnerIdAndDeletedAtIsNull(task.getId(), OWNER)).thenReturn(Optional.of(task));

		var dto = service.patch(task.getId(),
				patchOf("dueDate", new TaskPatchRequest(null, null, null, null, null, null, null)), null);

		assertThat(dto.dueDate()).isNull();
		assertThat(dto.description()).isEqualTo("keep me");
		assertThat(dto.priority()).isEqualTo(Priority.HIGH);
		assertThat(dto.title()).isEqualTo("existing");
	}

	@Test
	void patchRejectsBlankTitle() {
		Task task = existing(TaskStatus.TODO);
		when(tasks.findByIdAndOwnerIdAndDeletedAtIsNull(task.getId(), OWNER)).thenReturn(Optional.of(task));

		assertThatThrownBy(() -> service.patch(task.getId(),
				patchOf("title", new TaskPatchRequest(" ", null, null, null, null, null, null)), null))
			.isInstanceOf(BadRequestException.class);
	}

	@Test
	void staleVersionIsAConflictCarryingTheCurrentCopy() {
		Task task = existing(TaskStatus.TODO);
		when(tasks.findByIdAndOwnerIdAndDeletedAtIsNull(task.getId(), OWNER)).thenReturn(Optional.of(task));
		// a non-persisted entity has a null version, so any expected version is stale
		var update = new TaskUpdateRequest("x", null, null, Priority.LOW, TaskStatus.TODO, null, 7L);

		assertThatThrownBy(() -> service.update(task.getId(), update, 7L))
			.isInstanceOfSatisfying(ConflictException.class,
					e -> assertThat(e.getCurrent()).isInstanceOf(TaskDto.class));
		verify(tasks, never()).saveAndFlush(any());
	}

	@Test
	void unknownTaskIsNotFound() {
		UUID id = UUID.randomUUID();
		when(tasks.findByIdAndOwnerIdAndDeletedAtIsNull(id, OWNER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.get(id)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void restoreTakesTheTaskOutOfTheBin() {
		Task task = existing(TaskStatus.TODO);
		task.setDeletedAt(NOW.minusSeconds(60));
		when(tasks.findByIdAndOwnerId(task.getId(), OWNER)).thenReturn(Optional.of(task));

		TaskDto dto = service.restore(task.getId(), null);

		assertThat(dto.deletedAt()).isNull();
		assertThat(task.isDeleted()).isFalse();
	}

	@Test
	void restoreSendsATaskWhoseCategoryIsGoneToInbox() {
		Task task = existing(TaskStatus.TODO);
		task.setDeletedAt(NOW.minusSeconds(60));
		UUID gone = UUID.randomUUID();
		task.setCategoryId(gone);
		when(tasks.findByIdAndOwnerId(task.getId(), OWNER)).thenReturn(Optional.of(task));
		when(categories.findByIdAndOwnerIdAndDeletedAtIsNull(gone, OWNER)).thenReturn(Optional.empty());

		service.restore(task.getId(), null);

		assertThat(task.getCategoryId()).isNull();
	}

	@Test
	void restoreOfAPurgedTaskIsNotFound() {
		Task task = existing(TaskStatus.TODO);
		task.setDeletedAt(NOW.minusSeconds(60));
		task.setPurgedAt(NOW.minusSeconds(30));
		when(tasks.findByIdAndOwnerId(task.getId(), OWNER)).thenReturn(Optional.of(task));

		assertThatThrownBy(() -> service.restore(task.getId(), null)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void restoreOfALiveTaskIsANoOpAndSkipsTheVersionCheck() {
		Task task = existing(TaskStatus.TODO);
		when(tasks.findByIdAndOwnerId(task.getId(), OWNER)).thenReturn(Optional.of(task));

		assertThat(service.restore(task.getId(), 99L).id()).isEqualTo(task.getId());
		verify(tasks, never()).saveAndFlush(any(Task.class));
	}

	@Test
	void emptyBinPurgesForTheCurrentUserAtTheClockTime() {
		when(tasks.purgeBin(OWNER, NOW)).thenReturn(3);

		assertThat(service.emptyBin()).isEqualTo(3);
	}

	@Test
	void deleteIsSoftAndStampsDeletedAt() {
		Task task = existing(TaskStatus.TODO);
		when(tasks.findByIdAndOwnerId(task.getId(), OWNER)).thenReturn(Optional.of(task));

		service.delete(task.getId(), null);

		assertThat(task.getDeletedAt()).isEqualTo(NOW);
		verify(tasks).saveAndFlush(task);
	}

	@Test
	void deletingTwiceIsANoOp() {
		Task task = existing(TaskStatus.TODO);
		task.setDeletedAt(NOW.minusSeconds(60));
		when(tasks.findByIdAndOwnerId(task.getId(), OWNER)).thenReturn(Optional.of(task));

		service.delete(task.getId(), null);

		assertThat(task.getDeletedAt()).isEqualTo(NOW.minusSeconds(60));
		verify(tasks, never()).saveAndFlush(any());
	}

	@Test
	void deletingUnknownTaskIsNotFound() {
		UUID id = UUID.randomUUID();
		when(tasks.findByIdAndOwnerId(id, OWNER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(id, null)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void parseSortSupportsDescendingPrefixAndAddsTieBreaker() {
		var sort = TaskService.parseSort("priority,-dueDate");

		assertThat(sort.stream().map(o -> o.getProperty() + ":" + o.getDirection()).toList())
			.containsExactly("priority:ASC", "dueDate:DESC", "id:ASC");
	}

	@Test
	void parseSortRejectsUnknownField() {
		assertThatThrownBy(() -> TaskService.parseSort("password")).isInstanceOf(BadRequestException.class);
	}

	@Test
	void listRejectsOversizedPage() {
		assertThatThrownBy(() -> service.list(new TaskFilter(null, false, null, null, null), null, 0, 1000))
			.isInstanceOf(BadRequestException.class);
	}

}
