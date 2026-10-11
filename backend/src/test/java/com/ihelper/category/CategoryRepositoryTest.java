package com.ihelper.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.ihelper.TestcontainersConfiguration;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CategoryRepositoryTest {

	@Autowired
	CategoryRepository categories;

	private Category category(String name) {
		Category c = new Category();
		c.setId(UUID.randomUUID());
		c.setName(name);
		return categories.saveAndFlush(c);
	}

	@Test
	void flywaySeedsTheFiveDefaultCategories() {
		assertThat(categories.findAllByDeletedAtIsNullOrderByNameAsc()).extracting(Category::getName)
			.contains("Personal", "Work", "Home", "Shopping", "Ideas");
	}

	@Test
	void nameIsUniqueIgnoringCaseAmongLiveRows() {
		String name = "Unique-" + UUID.randomUUID();
		category(name);

		assertThat(categories.existsByNameIgnoreCaseAndDeletedAtIsNull(name.toUpperCase())).isTrue();
		assertThatThrownBy(() -> category(name.toLowerCase())).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void deletedCategoryFreesItsName() {
		String name = "Reusable-" + UUID.randomUUID();
		Category first = category(name);
		first.setDeletedAt(Instant.now());
		categories.saveAndFlush(first);

		assertThat(categories.existsByNameIgnoreCaseAndDeletedAtIsNull(name)).isFalse();
		assertThat(category(name).getId()).isNotEqualTo(first.getId());
	}

	@Test
	void persistingSetsTimestampsAndVersionZero() {
		Category c = category("Stamped-" + UUID.randomUUID());

		assertThat(c.getCreatedAt()).isNotNull();
		assertThat(c.getUpdatedAt()).isEqualTo(c.getCreatedAt());
		assertThat(c.getVersion()).isZero();
	}

}
