package com.github.albertus82.extfix.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tika.Tika;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.albertus82.extfix.Console;

class AnalyzerTest {

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
