package io.github.albertus82.extfix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtFixTest {

	private static final boolean YES = true;

	@Test
	void test() throws Throwable {
		final boolean links = true;
		final boolean dryRun = false;
		final boolean recursive = false;
		final boolean errors = true;
		FileTestUtils.runWithTempDir(path -> {
			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p4 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p5 = FileTestUtils.copyResourceToDir("png.jpg", path);

			final ExtFix e1 = new ExtFix(path, new String[] { "jpg", "png" }, links, dryRun, recursive, errors, YES);
			Assertions.assertEquals(0, e1.call());
			Assertions.assertFalse(Files.exists(p3), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p4), Arrays.toString(path.toFile().listFiles())); // "jpeg" is not included in Extensions
			Assertions.assertFalse(Files.exists(p5), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertEquals(3, Files.list(path).count());

			final ExtFix e2 = new ExtFix(path, new String[] { "jpeg" }, links, dryRun, recursive, errors, YES);
			Assertions.assertEquals(0, e2.call());
			Assertions.assertFalse(Files.exists(p4), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertEquals(3, Files.list(path).count());
		});
	}

	@Test
	void testWithoutExtension() throws Throwable {
		final boolean links = false;
		final boolean dryRun = false;
		final boolean recursive = true;
		final boolean errors = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg", path);
			final Path p2 = FileTestUtils.copyResourceToDir("png", path);
			final Path p3 = FileTestUtils.copyResourceToDir(".hidden", path);

			final ExtFix e1 = new ExtFix(path, null, links, dryRun, recursive, errors, YES);
			Assertions.assertEquals(0, e1.call());
			Assertions.assertFalse(Files.exists(p1), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertFalse(Files.exists(p2), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertFalse(Files.exists(p3), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(Paths.get(p1.toString() + ".jpg")), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(Paths.get(p2.toString() + ".png")), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(Paths.get(p3.toString() + ".txt")), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertEquals(3, Files.list(path).count());
		});
	}

	@Test
	void testNoProblem() throws Throwable {
		final boolean links = false;
		final boolean dryRun = false;
		final boolean recursive = false;
		final boolean errors = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.jpeg", path);
			final Path p2 = FileTestUtils.copyResourceToDir("jpeg.jpg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("png.png", path);

			final ExtFix e1 = new ExtFix(path, new String[0], links, dryRun, recursive, errors, YES);
			Assertions.assertEquals(0, e1.call());
			Assertions.assertTrue(Files.exists(p1), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p2), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p3), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertEquals(3, Files.list(path).count());
		});
	}

	@Test
	void testAutoRename() throws Throwable {
		final boolean links = false;
		final boolean dryRun = false;
		final boolean recursive = true;
		final boolean errors = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.jpeg", path);
			final Path p2 = FileTestUtils.copyResourceToDir("jpeg.jpg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p4 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p5 = FileTestUtils.copyResourceToDir("png.jpg", path);
			final Path p6 = FileTestUtils.copyResourceToDir("png.png", path);
			final Path p7 = FileTestUtils.copyResourceToDir("png (1).png", path);

			final ExtFix e1 = new ExtFix(path, new String[] { "jpg", "png" }, links, dryRun, recursive, errors, YES);
			Assertions.assertEquals(0, e1.call());
			Assertions.assertTrue(Files.exists(p1), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p2), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertFalse(Files.exists(p3), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p4), Arrays.toString(path.toFile().listFiles())); // "jpeg" is not included in Extensions
			Assertions.assertFalse(Files.exists(p5), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p6), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p7), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(Paths.get(p3.toString().replace("jpeg.png", "jpeg (1).jpg"))), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(Paths.get(p5.toString().replace("png.jpg", "png (2).png"))), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertEquals(7, Files.list(path).count());
		});
	}

	@Test
	void testDryRun() throws Throwable {
		final boolean links = false;
		final boolean dryRun = true;
		final boolean recursive = false;
		final boolean errors = false;
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.jpeg", path);
			final Path p2 = FileTestUtils.copyResourceToDir("jpeg.jpg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p4 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p5 = FileTestUtils.copyResourceToDir("png.jpg", path);
			final Path p6 = FileTestUtils.copyResourceToDir("png.png", path);

			final ExtFix e1 = new ExtFix(path, new String[] { "jpg", "png", "jpeg" }, links, dryRun, recursive, errors, YES);
			Assertions.assertEquals(0, e1.call());
			Assertions.assertTrue(Files.exists(p1), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p2), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p3), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p4), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p5), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertTrue(Files.exists(p6), Arrays.toString(path.toFile().listFiles()));
			Assertions.assertEquals(6, Files.list(path).count());
		});
	}

}
