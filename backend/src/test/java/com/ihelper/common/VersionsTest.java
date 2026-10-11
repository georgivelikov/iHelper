package com.ihelper.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class VersionsTest {

	@Test
	void bodyVersionWinsOverHeader() {
		assertThat(Versions.resolve(4L, "\"9\"")).isEqualTo(4L);
	}

	@Test
	void parsesQuotedAndWeakEtags() {
		assertThat(Versions.resolve(null, "\"3\"")).isEqualTo(3L);
		assertThat(Versions.resolve(null, "W/\"3\"")).isEqualTo(3L);
		assertThat(Versions.resolve(null, "3")).isEqualTo(3L);
	}

	@Test
	void absentIsNullAndRequireRejectsIt() {
		assertThat(Versions.resolve(null, null)).isNull();
		assertThatThrownBy(() -> Versions.require(null, null)).isInstanceOf(BadRequestException.class);
	}

	@Test
	void garbageIsABadRequest() {
		assertThatThrownBy(() -> Versions.resolve(null, "abc")).isInstanceOf(BadRequestException.class);
	}

}
