package com.github.albertus82.extfix.engine;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.albertus82.extfix.Console;
import com.github.albertus82.extfix.FileTestUtils;

class RenamerTest {

	private static final String RANDOM = UUID.randomUUID().toString().replace("-", "");

	@Test
	void testConstructor() {
		Assertions.assertThrows(NullPointerException.class, () -> new Renamer(null));
		Assertions.assertDoesNotThrow(() -> new Renamer(new Console(true)));
		Assertions.assertDoesNotThrow(() -> new Renamer(new Console(false)));
	}

	@Test
	void testRenameNonInteractive() {
		final boolean yes = true;
		final Renamer renamer = new Renamer(new Console(true));
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(null, false, yes));
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(null, true, yes));
		final Map<Path, String> m1 = Collections.singletonMap(null, null);
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m1, false, yes));
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m1, true, yes));
		final Map<Path, String> m2 = Collections.singletonMap(null, "ext");
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m2, false, yes));
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m2, true, yes));
		final Map<Path, String> m3 = Collections.singletonMap(Paths.get("/tmp/" + RANDOM + ".bar"), null);
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m3, false, yes));
		Assertions.assertThrows(NullPointerException.class, () -> renamer.rename(m3, true, yes));
	}

	@Test
	void testRenameInteractive() throws IOException {
		final boolean yes = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("y" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, true, Console.DEFAULT_WIDTH));
				renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.png")));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("Y" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, true, Console.DEFAULT_WIDTH));
				renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.png")));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("yes" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, true, Console.DEFAULT_WIDTH));
				renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.png")));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("YES" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, true, Console.DEFAULT_WIDTH));
				renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.png")));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
	}

}
