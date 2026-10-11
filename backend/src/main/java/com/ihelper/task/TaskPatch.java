package com.ihelper.task;

import java.util.Set;

/** A parsed PATCH: which fields the client sent, and their values. */
public record TaskPatch(Set<String> present, TaskPatchRequest values) {

	boolean has(String field) {
		return present.contains(field);
	}

}
