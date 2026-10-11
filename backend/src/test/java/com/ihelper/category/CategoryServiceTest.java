package com.ihelper.category;

import static org.assertj.core.api.Assertions.assertThat;
import static com.ihelper.TestUsers.currentUser;
import static com.ihelper.TestUsers.OWNER;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ihelper.common.ConflictException;
import com.ihelper.common.NotFoundException;
import com.ihelper.task.TaskRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

	private static final Instant NOW = Instant.parse("2026-03-01T10:00:00Z");

	@Mock
	CategoryRepository categories;

	@Mock
	TaskRepository tasks;

	CategoryService service;

	@BeforeEach
	void setUp() {
		service = new CategoryService(categories, tasks, Clock.fixed(NOW, ZoneOffset.UTC), currentUser());
		Mockito.lenient().when(categories.saveAndFlush(any(Category.class))).thenAnswer(i -> i.getArgument(0));
	}

	private static Category category(String name) {
		Category c = new Category();
		c.setId(UUID.randomUUID());
		c.setName(name);
		c.setOwnerId(OWNER);
		return c;
	}

	@Test
	void deleteMovesTasksToInboxThenSoftDeletes() {
		Category c = category("Work");
		when(categories.findByIdAndOwnerId(c.getId(), OWNER)).thenReturn(Optional.of(c));

		service.delete(c.getId(), null);

		var order = inOrder(tasks, categories);
		order.verify(tasks).moveToInbox(OWNER, c.getId(), NOW);
		order.verify(categories).saveAndFlush(c);
		assertThat(c.getDeletedAt()).isEqualTo(NOW);
	}

	@Test
	void deleteTwiceDoesNothing() {
		Category c = category("Work");
		c.setDeletedAt(NOW.minusSeconds(5));
		when(categories.findByIdAndOwnerId(c.getId(), OWNER)).thenReturn(Optional.of(c));

		service.delete(c.getId(), null);

		verify(tasks, never()).moveToInbox(any(), any(), any());
	}

	@Test
	void deleteUnknownIsNotFound() {
		UUID id = UUID.randomUUID();
		when(categories.findByIdAndOwnerId(id, OWNER)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(id, null)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void createRejectsDuplicateNameIgnoringCase() {
		when(categories.findById(any())).thenReturn(Optional.empty());
		when(categories.existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNull(OWNER, "work")).thenReturn(true);

		assertThatThrownBy(() -> service.create(new CategoryCreateRequest(null, "work", null)))
			.isInstanceOf(ConflictException.class);
	}

	@Test
	void createWithExistingIdIsIdempotentEvenIfNameIsTaken() {
		Category c = category("Work");
		when(categories.findById(c.getId())).thenReturn(Optional.of(c));

		var result = service.create(new CategoryCreateRequest(c.getId(), "Work", null));

		assertThat(result.created()).isFalse();
		verify(categories, never()).saveAndFlush(any());
	}

	@Test
	void updateRenamesAndChecksUniquenessExcludingSelf() {
		Category c = category("Work");
		when(categories.findByIdAndOwnerIdAndDeletedAtIsNull(c.getId(), OWNER)).thenReturn(Optional.of(c));
		when(categories.existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNullAndIdNot(OWNER, "WORK", c.getId())).thenReturn(false);

		var dto = service.update(c.getId(), new CategoryUpdateRequest("WORK", "#112233", null), null);

		assertThat(dto.name()).isEqualTo("WORK");
		assertThat(dto.color()).isEqualTo("#112233");
	}

	@Test
	void updateWithStaleVersionConflictsWithCurrentCopy() {
		Category c = category("Work");
		when(categories.findByIdAndOwnerIdAndDeletedAtIsNull(c.getId(), OWNER)).thenReturn(Optional.of(c));

		assertThatThrownBy(() -> service.update(c.getId(), new CategoryUpdateRequest("Other", null, 3L), 3L))
			.isInstanceOfSatisfying(ConflictException.class,
					e -> assertThat(e.getCurrent()).isInstanceOf(CategoryDto.class));
	}

}
