package com.github.albertus82.extfix.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.albertus82.extfix.Console;
import com.github.albertus82.extfix.FileTestUtils;

class AnalyzerTest {

	@Test
	void testDetectMediaType() throws IOException {
		FileTestUtils.runWithTempDir(path -> {
			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.jpg", path);
			final Path p2 = FileTestUtils.copyResourceToDir("jpeg.jpeg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p4 = FileTestUtils.copyResourceToDir("png.jpg", path);
			final Path p5 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p6 = FileTestUtils.copyResourceToDir("png.png", path);
			final Analyzer a = new Analyzer(new Console());
			Assertions.assertTrue(a.detectMediaType(p1).toLowerCase(Locale.ROOT).endsWith("jpeg"));
			Assertions.assertTrue(a.detectMediaType(p2).toLowerCase(Locale.ROOT).endsWith("jpeg"));
			Assertions.assertTrue(a.detectMediaType(p3).toLowerCase(Locale.ROOT).endsWith("jpeg"));
			Assertions.assertTrue(a.detectMediaType(p4).toLowerCase(Locale.ROOT).endsWith("png"));
			Assertions.assertTrue(a.detectMediaType(p5).toLowerCase(Locale.ROOT).endsWith("png"));
			Assertions.assertTrue(a.detectMediaType(p6).toLowerCase(Locale.ROOT).endsWith("png"));
		});
	}

	@Test
	void testFindBetterExtension() {
		Assertions.assertEquals(Optional.empty(), Analyzer.findBetterExtension(Paths.get("/tmp/foo.txt"), Arrays.asList(".txt")));
		Assertions.assertEquals(Optional.empty(), Analyzer.findBetterExtension(Paths.get("/tmp/foo.TXT"), Arrays.asList(".txt", ".bar")));
		Assertions.assertEquals(Optional.empty(), Analyzer.findBetterExtension(Paths.get("/tmp/foo.txt"), Arrays.asList(".FOO", ".TXT")));
		Assertions.assertEquals(Optional.empty(), Analyzer.findBetterExtension(Paths.get("/tmp/foo.TXT"), Arrays.asList(".TXT")));
		Assertions.assertTrue(Analyzer.findBetterExtension(Paths.get("/tmp/foo"), Arrays.asList(".txt")).get().toString().endsWith(".txt"));
		Assertions.assertTrue(Analyzer.findBetterExtension(Paths.get("/tmp/foo"), Arrays.asList(".BAR", ".txt")).get().toString().endsWith(".BAR"));
		Assertions.assertTrue(Analyzer.findBetterExtension(Paths.get("/tmp/foo.bar"), Arrays.asList(".lst", ".txt")).get().toString().endsWith(".lst"));
		final List<String> list = Collections.emptyList();
		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.findBetterExtension(null, list));
		final Path path = Paths.get("/tmp/foo.bar");
		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.findBetterExtension(path, null));
		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.findBetterExtension(null, null));
	}

}