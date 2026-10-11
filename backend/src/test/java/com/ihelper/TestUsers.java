package com.ihelper;

import java.util.UUID;

import com.ihelper.common.CurrentUser;

/** A fixed signed-in user for unit tests. */
public final class TestUsers {

	public static final UUID OWNER = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private TestUsers() {
	}

	public static CurrentUser currentUser() {
		return new CurrentUser() {
			@Override
			public UUID id() {
				return OWNER;
			}
		};
	}

}
