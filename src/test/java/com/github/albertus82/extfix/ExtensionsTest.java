package com.github.albertus82.extfix;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtensionsTest {

	private static final String RANDOM = UUID.randomUUID().toString().replace("-", "");

	@Test
	void testArray() throws IOException {
		final Extensions e1 = new Extensions(null, null);
		Assertions.assertDoesNotThrow(() -> e1.get());

		final Extensions e2 = new Extensions(new String[] { "pdf" }, Paths.get("/tmp/" + RANDOM + ".bar"));
		Assertions.assertThrows(IllegalStateException.class, () -> e2.get());

		final Extensions e3 = new Extensions(new String[] { "png", ".JPG" }, null);
		Assertions.assertArrayEquals(new String[] { ".jpg", ".png" }, e3.get());

		FileTestUtils.runWithTempDir(path -> {
			final Path target = FileTestUtils.copyResourceToDir("test-array-1.lst", path);
			final Extensions e4 = new Extensions(null, target);
			Assertions.assertArrayEquals(new String[] { ".jpeg", ".jpg", ".png" }, e4.get());
		});
	}

	@Test
	void testFromArray() {
		Assertions.assertThrows(NullPointerException.class, () -> Extensions.from((String[]) null));
		Assertions.assertEquals(Collections.emptySet(), Extensions.from());
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from(".pdf"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from("pdf"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from(".PDF"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from("PDF"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from(".pdf"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from(".pdf"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from(".jpg", ".jpeg"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from(".JPG", ".jpeg"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from(".jpg", ".JPEG"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from(".JPG", ".JPEG"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from("jpg", ".jpeg"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from("JPG", ".jpeg"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from("jpg", ".JPEG"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from("JPG", ".JPEG"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from(".jpg", "jpeg"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from(".JPG", "jpeg"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from(".jpg", "JPEG"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".jpg", ".jpeg")), Extensions.from(".JPG", "JPEG"));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf", ".png", ".jpg")), Extensions.from("jpg", ".png", "PDF", ".pdf"));
	}

	@Test
	void testFromPath() throws IOException {
		FileTestUtils.runWithTempDir(path -> {
			final Map<Integer, Path> fileMap = new HashMap<>();
			for (int i = 1; i <= 5; i++) {
				final Path target = FileTestUtils.copyResourceToDir("test-from-path-" + i + ".lst", path);
				fileMap.put(i, target);
			}
			Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from(fileMap.get(1)));
			Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from(fileMap.get(2)));
			Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from(fileMap.get(3)));
			Assertions.assertEquals(new HashSet<>(Arrays.asList(".pdf")), Extensions.from(fileMap.get(4)));
			Assertions.assertEquals(new HashSet<>(Arrays.asList(".png", ".jpg", ".jpeg")), Extensions.from(fileMap.get(5)));
		});
	}

}
