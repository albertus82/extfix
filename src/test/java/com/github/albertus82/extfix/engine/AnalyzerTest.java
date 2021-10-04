package com.github.albertus82.extfix.engine;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.albertus82.extfix.Console;
import com.github.albertus82.extfix.FileTestUtils;

class AnalyzerTest {

	private static final String RANDOM = UUID.randomUUID().toString().replace("-", "");

	@Test
	void testConstructor() {
		Assertions.assertThrows(NullPointerException.class, () -> new Analyzer(null));
		Assertions.assertDoesNotThrow(() -> new Analyzer(new Console()));
	}

	@Test
	void testAnalyze() throws IOException {
		final Analyzer a = new Analyzer(new Console());
		Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 1), a.analyze(Paths.get("tmp", RANDOM), false, false));
		Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 1), a.analyze(Paths.get("tmp", RANDOM), false, true));
		FileTestUtils.runWithTempDir(path -> {
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, false, false));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, false, true));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, false));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, true));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, true, (String[]) null));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, true, new String[] {}));
			Assertions.assertEquals(a.new AnalysisResult(Collections.emptyMap(), 0, 0), a.analyze(path, true, true, new String[] { "aaa", "bbb", "ccc" }));
		});
	}
	//
	//	@Test
	//	void testVisitor() throws IOException {
	//		final PathVisitor pv = new Analyzer(new Console());
	//		Assertions.assertThrows(NullPointerException.class, () -> pv.preVisitDirectory(null, null));
	//		Assertions.assertThrows(NullPointerException.class, () -> pv.visitFile(null, null));
	//		Assertions.assertEquals(FileVisitResult.CONTINUE, pv.preVisitDirectory(Paths.get("/tmp/" + RANDOM), null));
	//		Assertions.assertEquals(FileVisitResult.CONTINUE, pv.postVisitDirectory(null, null));
	//		Assertions.assertEquals(FileVisitResult.CONTINUE, pv.visitFile(Paths.get("/tmp/" + RANDOM), null));
	//		Assertions.assertEquals(FileVisitResult.CONTINUE, pv.visitFileFailed(null, null));
	//	}
	//
	//	@Test
	//	void testDetectMediaType() throws IOException {
	//		final Analyzer a = new Analyzer(new Console());
	//		Assertions.assertThrows(NullPointerException.class, () -> a.detectMediaType(null));
	//		FileTestUtils.runWithTempDir(path -> {
	//			final Path p1 = FileTestUtils.copyResourceToDir("jpeg.jpg", path);
	//			final Path p2 = FileTestUtils.copyResourceToDir("jpeg.jpeg", path);
	//			final Path p3 = FileTestUtils.copyResourceToDir("jpeg.png", path);
	//			final Path p4 = FileTestUtils.copyResourceToDir("png.jpg", path);
	//			final Path p5 = FileTestUtils.copyResourceToDir("png.jpeg", path);
	//			final Path p6 = FileTestUtils.copyResourceToDir("png.png", path);
	//			Assertions.assertTrue(a.detectMediaType(p1).toLowerCase(Locale.ROOT).endsWith("jpeg"));
	//			Assertions.assertTrue(a.detectMediaType(p2).toLowerCase(Locale.ROOT).endsWith("jpeg"));
	//			Assertions.assertTrue(a.detectMediaType(p3).toLowerCase(Locale.ROOT).endsWith("jpeg"));
	//			Assertions.assertTrue(a.detectMediaType(p4).toLowerCase(Locale.ROOT).endsWith("png"));
	//			Assertions.assertTrue(a.detectMediaType(p5).toLowerCase(Locale.ROOT).endsWith("png"));
	//			Assertions.assertTrue(a.detectMediaType(p6).toLowerCase(Locale.ROOT).endsWith("png"));
	//		});
	//	}
	//
	//	@Test
	//	void testFindBetterExtension() {
	//		Assertions.assertEquals(Optional.empty(), Analyzer.findBetterExtension(Paths.get("/tmp/" + RANDOM + ".txt"), Arrays.asList(".txt")));
	//		Assertions.assertEquals(Optional.empty(), Analyzer.findBetterExtension(Paths.get("/tmp/" + RANDOM + ".TXT"), Arrays.asList(".txt", ".bar")));
	//		Assertions.assertEquals(Optional.empty(), Analyzer.findBetterExtension(Paths.get("/tmp/" + RANDOM + ".txt"), Arrays.asList(".FOO", ".TXT")));
	//		Assertions.assertEquals(Optional.empty(), Analyzer.findBetterExtension(Paths.get("/tmp/" + RANDOM + ".TXT"), Arrays.asList(".TXT")));
	//		Assertions.assertTrue(Analyzer.findBetterExtension(Paths.get("/tmp/" + RANDOM), Arrays.asList(".txt")).get().toString().endsWith(".txt"));
	//		Assertions.assertTrue(Analyzer.findBetterExtension(Paths.get("/tmp/" + RANDOM), Arrays.asList(".BAR", ".txt")).get().toString().endsWith(".BAR"));
	//		Assertions.assertTrue(Analyzer.findBetterExtension(Paths.get("/tmp/" + RANDOM + ".bar"), Arrays.asList(".lst", ".txt")).get().toString().endsWith(".lst"));
	//		final List<String> list = Collections.emptyList();
	//		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.findBetterExtension(null, list));
	//		final Path path = Paths.get("/tmp/" + RANDOM + ".bar");
	//		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.findBetterExtension(path, null));
	//		Assertions.assertThrows(NullPointerException.class, () -> Analyzer.findBetterExtension(null, null));
	//	}

}
