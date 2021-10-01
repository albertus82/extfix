package com.github.albertus82.extfix;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.extern.java.Log;

@Log
class ExtensionsTest {

	@Test
	void testArray() throws IOException {
		final Extensions e1 = new Extensions(null, null);
		Assertions.assertThrows(IllegalStateException.class, () -> e1.array());

		final Extensions e2 = new Extensions(new String[] { "pdf" }, Path.of("/tmp/foo.bar"));
		Assertions.assertThrows(IllegalStateException.class, () -> e2.array());

		final Extensions e3 = new Extensions(new String[] { "png", ".JPG" }, null);
		Assertions.assertArrayEquals(new String[] { ".jpg", ".png" }, e3.array());

		TestRunner.withTempPath(path -> {
			final String fileName = "test-array-1.lst";
			final Path target = Path.of(path.toString(), fileName);
			try (final InputStream in = getClass().getResourceAsStream('/' + fileName)) {
				Files.copy(in, target);
				log.log(Level.INFO, "Created temporary file \"{0}\".", target);
			}
			final Extensions e4 = new Extensions(null, target);
			Assertions.assertArrayEquals(new String[] { ".jpeg", ".jpg", ".png" }, e4.array());
		});
	}

	@Test
	void testFromArray() {
		Assertions.assertThrows(NullPointerException.class, () -> Extensions.from((String[]) null));
		Assertions.assertEquals(Collections.emptySet(), Extensions.from());
		Assertions.assertEquals(Set.of(".pdf"), Extensions.from(".pdf"));
		Assertions.assertEquals(Set.of(".pdf"), Extensions.from("pdf"));
		Assertions.assertEquals(Set.of(".pdf"), Extensions.from(".PDF"));
		Assertions.assertEquals(Set.of(".pdf"), Extensions.from("PDF"));
		Assertions.assertEquals(Set.of(".pdf"), Extensions.from(".pdf"));
		Assertions.assertEquals(Set.of(".pdf"), Extensions.from(".pdf"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from(".jpg", ".jpeg"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from(".JPG", ".jpeg"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from(".jpg", ".JPEG"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from(".JPG", ".JPEG"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from("jpg", ".jpeg"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from("JPG", ".jpeg"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from("jpg", ".JPEG"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from("JPG", ".JPEG"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from(".jpg", "jpeg"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from(".JPG", "jpeg"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from(".jpg", "JPEG"));
		Assertions.assertEquals(Set.of(".jpg", ".jpeg"), Extensions.from(".JPG", "JPEG"));
		Assertions.assertEquals(Set.of(".pdf", ".png", ".jpg"), Extensions.from("jpg", ".png", "PDF", ".pdf"));
	}

	@Test
	void testFromPath() throws IOException {
		TestRunner.withTempPath(path -> {
			final Map<Integer, Path> fileMap = new HashMap<>();
			for (int i = 1; i <= 5; i++) {
				final String fileName = "test-from-path-" + i + ".lst";
				final Path target = Path.of(path.toString(), fileName);
				try (final InputStream in = getClass().getResourceAsStream('/' + fileName)) {
					Files.copy(in, target);
					fileMap.put(i, target);
					log.log(Level.INFO, "Created temporary file \"{0}\".", target);
				}
			}
			Assertions.assertEquals(Set.of(".pdf"), Extensions.from(fileMap.get(1)));
			Assertions.assertEquals(Set.of(".pdf"), Extensions.from(fileMap.get(2)));
			Assertions.assertEquals(Set.of(".pdf"), Extensions.from(fileMap.get(3)));
			Assertions.assertEquals(Set.of(".pdf"), Extensions.from(fileMap.get(4)));
			Assertions.assertEquals(Set.of(".png", ".jpg", ".jpeg"), Extensions.from(fileMap.get(5)));
		});
	}

}
