package com.github.albertus82.extfix.util;

import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PathUtilsTest {

	@Test
	void testAbsolute() {
		Assertions.assertThrows(NullPointerException.class, () -> PathUtils.absolute(null));
	}

	@Test
	void testHasExtension() {
		Assertions.assertThrows(NullPointerException.class, () -> PathUtils.hasExtension(null));
		Assertions.assertTrue(PathUtils.hasExtension(Paths.get("/tmp/foo/bar.aa")));
		Assertions.assertTrue(PathUtils.hasExtension(Paths.get("/tmp/foo/.bar.bb")));
		Assertions.assertTrue(PathUtils.hasExtension(Paths.get("/tmp/foo/..bar.ccc")));
		Assertions.assertFalse(PathUtils.hasExtension(Paths.get("/tmp/foo/bar")));
		Assertions.assertFalse(PathUtils.hasExtension(Paths.get("/tmp/foo/.bar")));
	}

}
