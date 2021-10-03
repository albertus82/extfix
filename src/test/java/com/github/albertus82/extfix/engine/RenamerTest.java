package com.github.albertus82.extfix.engine;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.albertus82.extfix.Console;

public class RenamerTest {

	@Test
	void testRename() {
		final Renamer renamer = new Renamer(new Console(), false);
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(null, null));
		final Path path = Paths.get("/tmp/foo.bar");
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(null, "ext"));
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(path, null));
	}

}
