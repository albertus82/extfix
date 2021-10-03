package com.github.albertus82.extfix.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PathUtilsTest {

	@Test
	void testAbsolute() {
		Assertions.assertThrows(NullPointerException.class, () -> PathUtils.absolute(null));
	}

}
