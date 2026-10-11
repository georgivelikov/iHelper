package com.ihelper.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

final class TaskSpecifications {

	private TaskSpecifications() {
	}

	static Specification<Task> matching(TaskFilter filter) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.isNull(root.get("deletedAt")));
			if (filter.inbox()) {
				predicates.add(cb.isNull(root.get("categoryId")));
			}
			else if (filter.categoryId() != null) {
				predicates.add(cb.equal(root.get("categoryId"), filter.categoryId()));
			}
			if (filter.priority() != null) {
				predicates.add(cb.equal(root.get("priority"), filter.priority()));
			}
			if (filter.status() != null) {
				predicates.add(cb.equal(root.get("status"), filter.status()));
			}
			if (filter.q() != null && !filter.q().isBlank()) {
				String pattern = "%" + escapeLike(filter.q().trim().toLowerCase()) + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("title")), pattern, '\\'),
						cb.like(cb.lower(root.get("description")), pattern, '\\')));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static String escapeLike(String s) {
		return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
