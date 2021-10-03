package com.github.albertus82.extfix.engine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.albertus82.extfix.Console;

class RenamerTest {

	@Test
	void testRename() {
		final Renamer renamer = new Renamer(new Console(), false);
		final Map<Path, String> m1 = Collections.singletonMap(null, null);
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m1));
		final Map<Path, String> m2 = Collections.singletonMap(null, "ext");
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m2));
		final Map<Path, String> m3 = Collections.singletonMap(Paths.get("/tmp/foo.bar"), null);
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m3));
	}

}
