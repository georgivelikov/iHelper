package com.ihelper.common;

/** Reads the expected entity version from the body or the {@code If-Match} header. */
public final class Versions {

	private Versions() {
	}

	public static Long resolve(Long bodyVersion, String ifMatch) {
		if (bodyVersion != null) {
			return bodyVersion;
		}
		if (ifMatch == null || ifMatch.isBlank()) {
			return null;
		}
		String raw = ifMatch.trim();
		if (raw.startsWith("W/")) {
			raw = raw.substring(2);
		}
		raw = raw.replace("\"", "");
		try {
			return Long.parseLong(raw);
		}
		catch (NumberFormatException e) {
			throw new BadRequestException("If-Match must be an entity version number, got: " + ifMatch);
		}
	}

	public static Long require(Long bodyVersion, String ifMatch) {
		Long version = resolve(bodyVersion, ifMatch);
		if (version == null) {
			throw new BadRequestException("version is required (in the body or as If-Match header)");
		}
		return version;
	}

}
