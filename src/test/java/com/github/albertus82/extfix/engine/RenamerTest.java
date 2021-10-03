package com.github.albertus82.extfix.engine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.albertus82.extfix.Console;

class RenamerTest {

	private static final String RANDOM = UUID.randomUUID().toString().replace("-", "");

	@Test
	void testConstructor() {
		Assertions.assertThrows(NullPointerException.class, () -> new Renamer(null, true));
		Assertions.assertDoesNotThrow(() -> new Renamer(new Console(), true));
		Assertions.assertDoesNotThrow(() -> new Renamer(new Console(), false));
	}

	@Test
	void testRename() {
		final Renamer renamer = new Renamer(new Console(), false);
		final Map<Path, String> m1 = Collections.singletonMap(null, null);
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m1));
		final Map<Path, String> m2 = Collections.singletonMap(null, "ext");
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m2));
		final Map<Path, String> m3 = Collections.singletonMap(Paths.get("/tmp/" + RANDOM + ".bar"), null);
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m3));
	}

}
