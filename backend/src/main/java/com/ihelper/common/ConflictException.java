package com.ihelper.common;

/** Version conflict or uniqueness clash. {@code current} is the server copy, when there is one. */
public class ConflictException extends RuntimeException {

	private final transient Object current;

	public ConflictException(String message, Object current) {
		super(message);
		this.current = current;
	}

	public ConflictException(String message) {
		this(message, null);
	}

	public Object getCurrent() {
		return current;
	}

}
