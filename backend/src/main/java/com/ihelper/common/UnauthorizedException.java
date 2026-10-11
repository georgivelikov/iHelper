package com.ihelper.common;

/** Credentials or tokens were missing, wrong or expired. Deliberately says nothing about which. */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}

}
