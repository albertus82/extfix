package com.github.albertus82.extfix;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.tika.Tika;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtFixTest {

	@Test
	void test() throws IOException {
		final Tika tika = new ExtFix().tika;
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
	void testGetSuffixes() {
		Assertions.assertNotNull(ExtFix.getSuffixes());
	}

	@Test
	void testFixFileName() {
		Assertions.assertEquals(Optional.empty(), ExtFix.fixFileName(Path.of("/tmp/foo.txt"), List.of(".txt")));
		Assertions.assertEquals(Optional.empty(), ExtFix.fixFileName(Path.of("/tmp/foo.TXT"), List.of(".txt", ".bar")));
		Assertions.assertEquals(Optional.empty(), ExtFix.fixFileName(Path.of("/tmp/foo.txt"), List.of(".FOO", ".TXT")));
		Assertions.assertEquals(Optional.empty(), ExtFix.fixFileName(Path.of("/tmp/foo.TXT"), List.of(".TXT")));
		Assertions.assertTrue(ExtFix.fixFileName(Path.of("/tmp/foo"), List.of(".txt")).get().toString().endsWith(".txt"));
		Assertions.assertTrue(ExtFix.fixFileName(Path.of("/tmp/foo"), List.of(".BAR", ".txt")).get().toString().endsWith(".BAR"));
		Assertions.assertTrue(ExtFix.fixFileName(Path.of("/tmp/foo.bar"), List.of(".lst", ".txt")).get().toString().endsWith(".lst"));
		Assertions.assertThrows(NullPointerException.class, () -> ExtFix.fixFileName(null, Collections.emptyList()));
		Assertions.assertThrows(NullPointerException.class, () -> ExtFix.fixFileName(Path.of("/tmp/foo.bar"), null));
		Assertions.assertThrows(NullPointerException.class, () -> ExtFix.fixFileName(null, null));
	}

}
