package com.github.albertus82.extfix;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tika.Tika;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtFixTest {

	private static final boolean YES = true;

	@Test
	void test() throws IOException {
		final boolean dryRun = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p4 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p5 = FileTestUtils.copyResourceToDir("png.jpg", path);

			final ExtFix e1 = new ExtFix(path, dryRun, YES, new Extensions(new String[] { "jpg", "png" }, null));
			e1.call();
			Assertions.assertFalse(Files.exists(p3));
			Assertions.assertTrue(Files.exists(p4)); // "jpeg" is not included in Extensions
			Assertions.assertFalse(Files.exists(p5));
			Assertions.assertEquals(3, Files.list(path).count());

			final ExtFix e2 = new ExtFix(path, dryRun, YES, new Extensions(new String[] { "jpeg" }, null));
			e2.call();
			Assertions.assertFalse(Files.exists(p4));
			Assertions.assertEquals(3, Files.list(path).count());
		});
	}

	@Test
	void testAutoRename() throws IOException {
		final boolean dryRun = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.jpeg", path);
			final Path p2 = FileTestUtils.copyResourceToDir("jpeg.jpg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p4 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p5 = FileTestUtils.copyResourceToDir("png.jpg", path);
			final Path p6 = FileTestUtils.copyResourceToDir("png.png", path);

			final ExtFix e1 = new ExtFix(path, dryRun, YES, new Extensions(new String[] { "jpg", "png" }, null));
			e1.call();
			Assertions.assertTrue(Files.exists(p1));
			Assertions.assertTrue(Files.exists(p2));
			Assertions.assertFalse(Files.exists(p3));
			Assertions.assertTrue(Files.exists(p4)); // "jpeg" is not included in Extensions
			Assertions.assertFalse(Files.exists(p5));
			Assertions.assertTrue(Files.exists(p6));
			Assertions.assertTrue(Files.exists(Paths.get(p3.toString().replace("jpeg.png", "jpeg (1).jpg"))));
			Assertions.assertTrue(Files.exists(Paths.get(p5.toString().replace("png.jpg", "png (1).png"))));
			Assertions.assertEquals(6, Files.list(path).count());
		});
	}

	@Test
	void testDryRun() throws IOException {
		final boolean dryRun = true;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.jpeg", path);
			final Path p2 = FileTestUtils.copyResourceToDir("jpeg.jpg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p4 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p5 = FileTestUtils.copyResourceToDir("png.jpg", path);
			final Path p6 = FileTestUtils.copyResourceToDir("png.png", path);

			final ExtFix e1 = new ExtFix(path, dryRun, YES, new Extensions(new String[] { "jpg", "png", "jpeg" }, null));
			e1.call();
			Assertions.assertTrue(Files.exists(p1));
			Assertions.assertTrue(Files.exists(p2));
			Assertions.assertTrue(Files.exists(p3));
			Assertions.assertTrue(Files.exists(p4));
			Assertions.assertTrue(Files.exists(p5));
			Assertions.assertTrue(Files.exists(p6));
			Assertions.assertEquals(6, Files.list(path).count());
		});
	}

	@Test
	void testTika() throws IOException {
		final Tika tika = new Analyzer(new Console()).getTika();
		try (final InputStream is = getClass().getResourceAsStream("/jpeg.jpg")) {
			Assertions.assertTrue(tika.detect(is).endsWith("jpeg"));
		}
		try (final InputStream is = getClass().getResourceAsStream("/jpeg.jpeg")) {
			Assertions.assertTrue(tika.detect(is).endsWith("jpeg"));
		}
		try (final InputStream is = getClass().getResourceAsStream("/jpeg.png")) {
			Assertions.assertTrue(tika.detect(is).endsWith("jpeg"));
		}
		try (final InputStream is = getClass().getResourceAsStream("/png.jpg")) {
			Assertions.assertTrue(tika.detect(is).endsWith("png"));
		}
		try (final InputStream is = getClass().getResourceAsStream("/png.jpeg")) {
			Assertions.assertTrue(tika.detect(is).endsWith("png"));
		}
		try (final InputStream is = getClass().getResourceAsStream("/png.png")) {
			Assertions.assertTrue(tika.detect(is).endsWith("png"));
		}
	}

	@Test
	void testFixFileName() {
		Assertions.assertEquals(Optional.empty(), Analyzer.fixFileName(Paths.get("/tmp/foo.txt"), Arrays.asList(".txt")));
		Assertions.assertEquals(Optional.empty(), Analyzer.fixFileName(Paths.get("/tmp/foo.TXT"), Arrays.asList(".txt", ".bar")));
		Assertions.assertEquals(Optional.empty(), Analyzer.fixFileName(Paths.get("/tmp/foo.txt"), Arrays.asList(".FOO", ".TXT")));
		Assertions.assertEquals(Optional.empty(), Analyzer.fixFileName(Paths.get("/tmp/foo.TXT"), Arrays.asList(".TXT")));
		Assertions.assertTrue(Analyzer.fixFileName(Paths.get("/tmp/foo"), Arrays.asList(".txt")).get().toString().endsWith(".txt"));
		Assertions.assertTrue(Analyzer.fixFileName(Paths.get("/tmp/foo"), Arrays.asList(".BAR", ".txt")).get().toString().endsWith(".BAR"));
		Assertions.assertTrue(Analyzer.fixFileName(Paths.get("/tmp/foo.bar"), Arrays.asList(".lst", ".txt")).get().toString().endsWith(".lst"));
		final List<String> list = Collections.emptyList();
		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.fixFileName(null, list));
		final Path path = Paths.get("/tmp/foo.bar");
		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.fixFileName(path, null));
		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.fixFileName(null, null));
	}

	@Test
	void testRename() {
		final Renamer extFix = new Renamer(new Console(), false);
		Assertions.assertThrows(NullPointerException.class, () -> extFix.rename(null, null));
		final Path path = Paths.get("/tmp/foo.bar");
		Assertions.assertThrows(NullPointerException.class, () -> extFix.rename(null, path));
		Assertions.assertThrows(NullPointerException.class, () -> extFix.rename(path, null));
	}

}
