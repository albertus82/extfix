package com.github.albertus82.extfix.engine;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.albertus82.extfix.Console;
import com.github.albertus82.extfix.FileTestUtils;
import com.github.albertus82.extfix.engine.Renamer.RenameResult;

class RenamerTest {

	private static final String RANDOM = UUID.randomUUID().toString().replace("-", "");

	@Test
	void testConstructor() {
		Assertions.assertThrows(NullPointerException.class, () -> new Renamer(null));
		Assertions.assertDoesNotThrow(() -> new Renamer(new Console(true)));
		Assertions.assertDoesNotThrow(() -> new Renamer(new Console(false)));
	}

	@Test
	void testRenameNonInteractive() throws Throwable {
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
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p2 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Map<Path, String> m4 = new HashMap<>();
			m4.put(p1, ".jpg");
			m4.put(p2, ".png");
			final RenameResult result = renamer.rename(m4, false, yes);
			Assertions.assertEquals(renamer.new RenameResult(2, 0, 0), result);
			Assertions.assertFalse(Files.exists(p1));
			Assertions.assertFalse(Files.exists(p2));
			Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "png.png")));
		});
	}

	@Test
	void testRenameInteractiveYes() throws Throwable {
		final boolean yes = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("y" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(1, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("Y" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(1, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("yes" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(1, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("YES" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(1, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
	}

	@Test
	void testRenameInteractiveNo() throws Throwable {
		final boolean yes = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("n" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("N" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("no" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("NO" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
	}

	@Test
	void testRenameInteractiveAll() throws Throwable {
		final boolean yes = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("a" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(1, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("A" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(1, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("all" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(1, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("ALL" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(1, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p2 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("png.jpg", path);
			final Map<Path, String> m1 = new HashMap<>();
			m1.put(p1, ".jpg");
			m1.put(p2, ".png");
			m1.put(p3, ".png");
			try (final InputStream is = new ByteArrayInputStream(("a" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(m1, false, yes);
				Assertions.assertEquals(renamer.new RenameResult(3, 0, 0), result);
				Assertions.assertFalse(Files.exists(p1));
				Assertions.assertFalse(Files.exists(p2));
				Assertions.assertFalse(Files.exists(p3));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "png.png")));
				Assertions.assertTrue(Files.exists(Paths.get(path.toString(), "png (1).png")));
			}
		});
	}

	@Test
	void testRenameInteractiveCancel() throws Throwable {
		final boolean yes = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("c" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("C" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("cancel" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("CANCEL" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p2 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("png.jpg", path);
			final Map<Path, String> m1 = new HashMap<>();
			m1.put(p1, ".jpg");
			m1.put(p2, ".png");
			m1.put(p3, ".png");
			try (final InputStream is = new ByteArrayInputStream(("c" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(m1, false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 3), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertTrue(Files.exists(p2));
				Assertions.assertTrue(Files.exists(p3));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "png.png")));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "png (1).png")));
			}
		});
	}

	@Test
	void testRenameInteractiveDefault() throws Throwable {
		final boolean yes = false;
		// Unrecognized input -> NO
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream(("qwertyuiop" + System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
		// [Enter] without any input -> NO
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			try (final InputStream is = new ByteArrayInputStream((System.lineSeparator()).getBytes())) {
				final Renamer renamer = new Renamer(new Console(is, System.out, System.err, true, Console.DEFAULT_WIDTH));
				final RenameResult result = renamer.rename(Collections.singletonMap(p1, ".jpg"), false, yes);
				Assertions.assertEquals(renamer.new RenameResult(0, 0, 1), result);
				Assertions.assertTrue(Files.exists(p1));
				Assertions.assertFalse(Files.exists(Paths.get(path.toString(), "jpeg.jpg")));
			}
		});
	}

}
