package com.github.albertus82.extfix;

import java.io.IOException;
import java.io.InputStream;

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
	void testFixFileName() {

	}

}
