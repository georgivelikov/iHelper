package com.ihelper.task;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import com.ihelper.TestcontainersConfiguration;
import com.ihelper.auth.User;
import com.ihelper.auth.UserRepository;
import com.ihelper.category.Category;
import com.ihelper.category.CategoryRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TaskRepositoryTest {

	@Autowired
	TaskRepository tasks;

	@Autowired
	CategoryRepository categories;

	@Autowired
	jakarta.persistence.EntityManager em;

	@Autowired
	UserRepository users;

	UUID owner;

	UUID otherOwner;

	@BeforeEach
	void createOwners() {
		owner = newUser();
		otherOwner = newUser();
	}

	private UUID newUser() {
		User u = new User();
		u.setId(UUID.randomUUID());
		u.setUsername("repo-" + UUID.randomUUID().toString().substring(0, 12));
		u.setPasswordHash("!");
		return users.saveAndFlush(u).getId();
	}

	private Task task(String title, Priority priority, TaskStatus status, LocalDate due, UUID categoryId) {
		Task t = new Task();
		t.setId(UUID.randomUUID());
		t.setOwnerId(owner);
		t.setTitle(title);
		t.setPriority(priority);
		t.setStatus(status);
		t.setDueDate(due);
		t.setCategoryId(categoryId);
		return tasks.saveAndFlush(t);
	}

	private Category category(String name) {
		Category c = new Category();
		c.setId(UUID.randomUUID());
		c.setOwnerId(owner);
		c.setName(name);
		return categories.saveAndFlush(c);
	}

	private List<String> titles(TaskFilter filter, String sort) {
		return tasks.findAll(TaskSpecifications.matching(owner, filter),
				PageRequest.of(0, 100, TaskService.parseSort(sort))).map(Task::getTitle).toList();
	}

	private static TaskFilter filter(UUID categoryId, Priority p, TaskStatus s, String q) {
		return new TaskFilter(categoryId, false, p, s == null ? null : java.util.Set.of(s), q);
	}

	@Test
	void filtersByCategoryPriorityStatusAndText() {
		Category cat = category("repo-test-" + UUID.randomUUID());
		task("alpha write report", Priority.HIGH, TaskStatus.TODO, null, cat.getId());
		task("beta call mom", Priority.LOW, TaskStatus.DONE, null, cat.getId());
		task("gamma unrelated", Priority.HIGH, TaskStatus.TODO, null, null);

		assertThat(titles(filter(cat.getId(), null, null, null), "title"))
			.containsExactly("alpha write report", "beta call mom");
		assertThat(titles(filter(cat.getId(), Priority.HIGH, null, null), "title"))
			.containsExactly("alpha write report");
		assertThat(titles(filter(cat.getId(), null, TaskStatus.DONE, null), "title"))
			.containsExactly("beta call mom");
		assertThat(titles(filter(null, null, null, "CALL MOM"), "title")).containsExactly("beta call mom");
	}

	@Test
	void inboxFilterSelectsUncategorizedTasks() {
		String marker = "inbox-" + UUID.randomUUID();
		Category cat = category("repo-test-" + UUID.randomUUID());
		task(marker + " in inbox", Priority.LOW, TaskStatus.TODO, null, null);
		task(marker + " filed", Priority.LOW, TaskStatus.TODO, null, cat.getId());

		var inbox = new TaskFilter(null, true, null, null, marker);
		assertThat(titles(inbox, null)).containsExactly(marker + " in inbox");
	}

	@Test
	void likeWildcardsInSearchAreLiteral() {
		String marker = UUID.randomUUID().toString();
		task(marker + " 100% done", Priority.LOW, TaskStatus.TODO, null, null);
		task(marker + " 100 done", Priority.LOW, TaskStatus.TODO, null, null);

		assertThat(titles(filter(null, null, null, marker + " 100%"), null)).containsExactly(marker + " 100% done");
	}

	@Test
	void softDeletedTasksAreExcluded() {
		String marker = "gone-" + UUID.randomUUID();
		Task t = task(marker, Priority.LOW, TaskStatus.TODO, null, null);
		t.setDeletedAt(Instant.now());
		tasks.saveAndFlush(t);

		assertThat(titles(filter(null, null, null, marker), null)).isEmpty();
		assertThat(tasks.findByIdAndOwnerIdAndDeletedAtIsNull(t.getId(), owner)).isEmpty();
		assertThat(tasks.findById(t.getId())).isPresent();
	}

	@Test
	void sortsByPriorityRankNotAlphabetically() {
		String marker = "prio-" + UUID.randomUUID();
		task(marker + " low", Priority.LOW, TaskStatus.TODO, null, null);
		task(marker + " urgent", Priority.URGENT, TaskStatus.TODO, null, null);
		task(marker + " medium", Priority.MEDIUM, TaskStatus.TODO, null, null);
		task(marker + " high", Priority.HIGH, TaskStatus.TODO, null, null);

		assertThat(titles(filter(null, null, null, marker), "-priority")).containsExactly(marker + " urgent",
				marker + " high", marker + " medium", marker + " low");
		assertThat(titles(filter(null, null, null, marker), "priority")).containsExactly(marker + " low",
				marker + " medium", marker + " high", marker + " urgent");
	}

	@Test
	void dueDateSortPutsTasksWithoutDueDateLast() {
		String marker = "due-" + UUID.randomUUID();
		task(marker + " none", Priority.LOW, TaskStatus.TODO, null, null);
		task(marker + " late", Priority.LOW, TaskStatus.TODO, LocalDate.of(2026, 12, 1), null);
		task(marker + " soon", Priority.LOW, TaskStatus.TODO, LocalDate.of(2026, 1, 1), null);

		assertThat(titles(filter(null, null, null, marker), "dueDate")).containsExactly(marker + " soon",
				marker + " late", marker + " none");
		assertThat(titles(filter(null, null, null, marker), "-dueDate")).containsExactly(marker + " late",
				marker + " soon", marker + " none");
	}

	@Test
	void paginationReportsTotal() {
		String marker = "page-" + UUID.randomUUID();
		for (int i = 0; i < 5; i++) {
			task(marker + " " + i, Priority.LOW, TaskStatus.TODO, null, null);
		}

		var page = tasks.findAll(TaskSpecifications.matching(owner, filter(null, null, null, marker)),
				PageRequest.of(1, 2, TaskService.parseSort("title")));

		assertThat(page.getTotalElements()).isEqualTo(5);
		assertThat(page.getContent()).extracting(Task::getTitle).containsExactly(marker + " 2", marker + " 3");
	}

	@Test
	void moveToInboxBumpsVersionAndUpdatedAt() {
		Category cat = category("repo-test-" + UUID.randomUUID());
		Task t = task("to move", Priority.LOW, TaskStatus.TODO, null, cat.getId());
		Instant before = t.getUpdatedAt();
		Instant now = before.plusSeconds(5);

		int moved = tasks.moveToInbox(owner, cat.getId(), now);
		em.clear(); // bulk update bypasses the persistence context
		Task reloaded = tasks.findById(t.getId()).orElseThrow();

		assertThat(moved).isEqualTo(1);
		assertThat(reloaded.getCategoryId()).isNull();
		assertThat(reloaded.getVersion()).isEqualTo(1L);
		assertThat(reloaded.getUpdatedAt()).isEqualTo(now);
	}

	@Test
	void changedSinceIncludesSoftDeleted() {
		Instant since = Instant.now().minusSeconds(1);
		Task t = task("sync-" + UUID.randomUUID(), Priority.LOW, TaskStatus.TODO, null, null);
		t.setDeletedAt(Instant.now());
		tasks.saveAndFlush(t);

		assertThat(tasks.findByOwnerIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(owner, since))
			.extracting(Task::getId).contains(t.getId());
	}

	@Test
	void queriesNeverReturnAnotherUsersRows() {
		String marker = "mine-" + UUID.randomUUID();
		Task mine = task(marker, Priority.LOW, TaskStatus.TODO, null, null);

		assertThat(tasks.findByIdAndOwnerIdAndDeletedAtIsNull(mine.getId(), otherOwner)).isEmpty();
		assertThat(tasks.findByIdAndOwnerId(mine.getId(), otherOwner)).isEmpty();
		assertThat(tasks.findAll(TaskSpecifications.matching(otherOwner, filter(null, null, null, marker)))).isEmpty();
		assertThat(tasks.findByOwnerIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(otherOwner, Instant.EPOCH))
			.extracting(Task::getId).doesNotContain(mine.getId());
		assertThat(tasks.countOpenByCategory(otherOwner)).isEmpty();
	}

	@Test
	void moveToInboxOnlyTouchesTheOwnersTasks() {
		Category cat = category("repo-test-" + UUID.randomUUID());
		Task t = task("stay", Priority.LOW, TaskStatus.TODO, null, cat.getId());

		int moved = tasks.moveToInbox(otherOwner, cat.getId(), Instant.now());
		em.clear();

		assertThat(moved).isZero();
		assertThat(tasks.findById(t.getId()).orElseThrow().getCategoryId()).isEqualTo(cat.getId());
	}

}
