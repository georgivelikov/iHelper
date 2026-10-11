package com.ihelper.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.ihelper.TestcontainersConfiguration;
import com.ihelper.auth.User;
import com.ihelper.auth.UserRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CategoryRepositoryTest {

	@Autowired
	CategoryRepository categories;

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

	private Category category(String name) {
		Category c = new Category();
		c.setId(UUID.randomUUID());
		c.setOwnerId(owner);
		c.setName(name);
		return categories.saveAndFlush(c);
	}

	@Test
	void flywaySeedsTheFiveDefaultCategoriesForThePlaceholderUser() {
		UUID placeholder = UUID.fromString("00000000-0000-0000-0000-000000000001");
		assertThat(categories.findAllByOwnerIdAndDeletedAtIsNullOrderByNameAsc(placeholder)).extracting(Category::getName)
			.contains("Personal", "Work", "Home", "Shopping", "Ideas");
	}

	@Test
	void nameIsUniqueIgnoringCaseAmongLiveRows() {
		String name = "Unique-" + UUID.randomUUID();
		category(name);

		assertThat(categories.existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNull(owner, name.toUpperCase())).isTrue();
		assertThatThrownBy(() -> category(name.toLowerCase())).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletedCategoryFreesItsName() {
		String name = "Reusable-" + UUID.randomUUID();
		Category first = category(name);
		first.setDeletedAt(Instant.now());
		categories.saveAndFlush(first);

		assertThat(categories.existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNull(owner, name)).isFalse();
		assertThat(category(name).getId()).isNotEqualTo(first.getId());
	}

	@Test
	void persistingSetsTimestampsAndVersionZero() {
		Category c = category("Stamped-" + UUID.randomUUID());

		assertThat(c.getCreatedAt()).isNotNull();
		assertThat(c.getUpdatedAt()).isEqualTo(c.getCreatedAt());
		assertThat(c.getVersion()).isZero();
	}

	@Test
	void sameNameIsAllowedForDifferentOwners() {
		String name = "Shared-" + UUID.randomUUID();
		category(name);

		Category other = new Category();
		other.setId(UUID.randomUUID());
		other.setOwnerId(otherOwner);
		other.setName(name);

		assertThat(categories.saveAndFlush(other).getId()).isNotNull();
		assertThat(categories.existsByOwnerIdAndNameIgnoreCaseAndDeletedAtIsNull(otherOwner, name)).isTrue();
	}

	@Test
	void lookupsAreScopedToTheOwner() {
		Category mine = category("Scoped-" + UUID.randomUUID());

		assertThat(categories.findByIdAndOwnerIdAndDeletedAtIsNull(mine.getId(), owner)).isPresent();
		assertThat(categories.findByIdAndOwnerIdAndDeletedAtIsNull(mine.getId(), otherOwner)).isEmpty();
		assertThat(categories.findAllByOwnerIdAndDeletedAtIsNullOrderByNameAsc(otherOwner)).isEmpty();
	}

}
