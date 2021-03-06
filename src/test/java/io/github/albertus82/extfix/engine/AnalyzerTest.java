package io.github.albertus82.extfix.engine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.albertus82.extfix.Console;
import io.github.albertus82.extfix.FileTestUtils;

class AnalyzerTest {

	private static final String RANDOM = UUID.randomUUID().toString().replace("-", "");

	@Test
	void testConstructor() {
		Assertions.assertThrows(NullPointerException.class, () -> new Analyzer(null));
		Assertions.assertDoesNotThrow(() -> new Analyzer(new Console(true)));
		Assertions.assertDoesNotThrow(() -> new Analyzer(new Console(false)));
	}

	@Test
	void testAnalyzeApi() {
		final Analyzer a = new Analyzer(new Console(true));
		Assertions.assertThrows(NullPointerException.class, () -> a.analyze(null, false, false));
	}

	@Test
	void testAnalyzeEmpty() throws Throwable {
		final Analyzer a = new Analyzer(new Console(false));
		FileTestUtils.runWithTempDir(path -> {
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, false, false));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, false, true));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, false));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, true));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, true, (String[]) null));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, true, new String[] {}));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, true, "aaa", "bbb", "ccc"));
		});
	}

	@Test
	void testAnalyze() throws Throwable {
		final Analyzer a = new Analyzer(new Console(true));
		Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 1), a.analyze(Paths.get("tmp", RANDOM), false, false));
		Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 1), a.analyze(Paths.get("tmp", RANDOM), false, true));
		FileTestUtils.runWithTempDir(path -> {
			FileTestUtils.copyResourceToDir("jpeg.jpeg", path);
			FileTestUtils.copyResourceToDir("jpeg.jpg", path);
			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
			final Path p4 = FileTestUtils.copyResourceToDir("png.jpeg", path);
			final Path p5 = FileTestUtils.copyResourceToDir("png.jpg", path);
			FileTestUtils.copyResourceToDir("png.png", path);

			final Map<Path, String> m1 = new HashMap<>();
			m1.put(p3, ".jpg");
			m1.put(p4, ".png");
			m1.put(p5, ".png");
			Assertions.assertEquals(a.new AnalysisResult(m1, 6, 0), a.analyze(path, false, false));
			Assertions.assertEquals(a.new AnalysisResult(m1, 6, 0), a.analyze(path, false, true));
			Assertions.assertEquals(a.new AnalysisResult(m1, 6, 0), a.analyze(path, true, false));
			Assertions.assertEquals(a.new AnalysisResult(m1, 6, 0), a.analyze(path, true, true));
			Assertions.assertEquals(a.new AnalysisResult(m1, 6, 0), a.analyze(path, false, false, ".png", ".jpg", ".jpeg"));
			Assertions.assertEquals(a.new AnalysisResult(m1, 6, 0), a.analyze(path, false, true, ".png", ".jpg", ".jpeg"));
			Assertions.assertEquals(a.new AnalysisResult(m1, 6, 0), a.analyze(path, true, false, ".png", ".jpg", ".jpeg"));
			Assertions.assertEquals(a.new AnalysisResult(m1, 6, 0), a.analyze(path, true, true, ".png", ".jpg", ".jpeg"));

			final Map<Path, String> m2 = new HashMap<>();
			m2.put(p3, ".jpg");
			Assertions.assertEquals(a.new AnalysisResult(m2, 2, 0), a.analyze(path, false, false, ".png"));
			Assertions.assertEquals(a.new AnalysisResult(m2, 2, 0), a.analyze(path, false, true, ".png"));
			Assertions.assertEquals(a.new AnalysisResult(m2, 2, 0), a.analyze(path, true, false, ".png"));
			Assertions.assertEquals(a.new AnalysisResult(m2, 2, 0), a.analyze(path, true, true, ".png"));

			final Map<Path, String> m3 = new HashMap<>();
			m3.put(p3, ".jpg");
			m3.put(p4, ".png");
			Assertions.assertEquals(a.new AnalysisResult(m3, 4, 0), a.analyze(path, false, false, ".png", ".jpeg"));
			Assertions.assertEquals(a.new AnalysisResult(m3, 4, 0), a.analyze(path, false, true, ".png", ".jpeg"));
			Assertions.assertEquals(a.new AnalysisResult(m3, 4, 0), a.analyze(path, true, false, ".png", ".jpeg"));
			Assertions.assertEquals(a.new AnalysisResult(m3, 4, 0), a.analyze(path, true, true, ".png", ".jpeg"));
		});
	}

	@Test
	void testExclusions() throws Throwable {
		final Analyzer a = new Analyzer(new Console(true));
		FileTestUtils.runWithTempDir(path -> {
			final Path excluded1 = Paths.get(path.toString(), "SYSTEM VOLUME INFORMATION");
			Files.createDirectories(excluded1);
			FileTestUtils.copyResourceToDir("jpeg.jpeg", excluded1);
			FileTestUtils.copyResourceToDir("jpeg.jpg", excluded1);

			final Path excluded2 = Paths.get(path.toString(), "$RECYCLE.BIN");
			Files.createDirectories(excluded2);
			FileTestUtils.copyResourceToDir("jpeg.jpeg", excluded2);

			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 3), a.analyze(path, false, true));
		});
	}

}
