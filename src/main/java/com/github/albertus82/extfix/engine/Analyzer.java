package com.github.albertus82.extfix.engine;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.file.PathFilter;
import org.apache.commons.io.file.PathVisitor;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MimeTypeException;

import com.github.albertus82.extfix.Console;
import com.github.albertus82.extfix.util.PathUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Analyzer {

	private static final Map<Path, Closeable> closeables = new ConcurrentHashMap<>();

	static {
		final Thread shutdownHook = new Thread(() -> {
			for (final Entry<Path, Closeable> entry : closeables.entrySet()) {
				try {
					entry.getValue().close();
				}
				catch (final IOException e) {
					e.printStackTrace();
				}
			}
		});
		shutdownHook.setPriority(Thread.MAX_PRIORITY);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
	private final Tika tika = new Tika(tikaConfig);

	@NonNull
	private final Console out;

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	public class AnalysisResult {
		@NonNull
		Map<Path, String> renameMap;

		int analyzedCount;
		int skippedCount;
	}

	public AnalysisResult analyze(@NonNull final Path path, final boolean links, final boolean recursive, final String... suffixes) throws IOException {
		final Visitor visitor = new Visitor(buildPathFilter(suffixes));
		if (recursive) {
			Files.walkFileTree(path, links ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : Collections.emptySet(), Short.MAX_VALUE, visitor);
		}
		else {
			visitor.preVisitDirectory(path, null);
			try (final Stream<Path> stream = Files.list(path)) {
				stream.forEach(entry -> {
					try {
						final BasicFileAttributes attrs = links ? Files.readAttributes(entry, BasicFileAttributes.class) : Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
						visitor.visitFile(entry, attrs);
					}
					catch (final IOException e) {
						visitor.visitFileFailed(entry, e);
					}
				});
			}
			catch (final IOException e) {
				visitor.visitFileFailed(path, e);
			}
		}
		out.clearAnalysisLine();
		return new AnalysisResult(Collections.unmodifiableMap(visitor.getRenameMap()), visitor.getAnalyzedCount(), visitor.getSkippedCount());
	}

	private PathFilter buildPathFilter(final String... suffixes) {
		if (suffixes != null && suffixes.length > 0) {
			out.printLine("Extensions: " + Arrays.toString(suffixes) + '.');
			return new SuffixFileFilter(suffixes, IOCase.INSENSITIVE);
		}
		else {
			return (p, a) -> FileVisitResult.CONTINUE;
		}
	}

	@RequiredArgsConstructor
	private class Visitor implements PathVisitor {

		@NonNull
		private final PathFilter pathFilter;

		@Getter
		private final Map<Path, String> renameMap = new TreeMap<>();

		@Getter
		private int analyzedCount;

		@Getter
		private int skippedCount;

		@Override
		public FileVisitResult preVisitDirectory(@NonNull final Path dir, final BasicFileAttributes attrs) {
			out.printAnalysisProgress(dir);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(@NonNull Path path, final BasicFileAttributes attrs) {
			if (FileVisitResult.CONTINUE.equals(pathFilter.accept(path, attrs)) && FileVisitResult.CONTINUE.equals(FileFileFilter.INSTANCE.accept(path, attrs))) {
				if (FileVisitResult.CONTINUE.equals(CanReadFileFilter.CAN_READ.accept(path, attrs))) {
					analyze(PathUtils.absolute(path));
				}
				else {
					skippedCount++;
					out.printAnalysisMessage("Skipping not readable file '" + path + "'.");
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(final Path file, final IOException e) {
			skippedCount++;
			if (e != null) {
				log.debug(String.valueOf(file), e);
			}
			out.printAnalysisError("Skipping '" + file + "' due to an exception: " + e, e);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(final Path dir, final IOException e) {
			if (e != null) {
				log.debug(String.valueOf(dir), e);
			}
			return FileVisitResult.CONTINUE;
		}

		private void analyze(@NonNull final Path path) {
			try {
				final String mediaType = detectMediaType(path);
				if (mediaType == null) {
					out.printAnalysisMessage("Cannot detect media type of '" + path + "'.");
				}
				else {
					final List<String> knownExtensions = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
					log.debug("{} <- {}", knownExtensions, path);
					if (knownExtensions.isEmpty()) {
						out.printAnalysisMessage("Cannot determine extension for '" + path + "'.");
					}
					else {
						findBetterExtension(path, knownExtensions).ifPresent(extension -> {
							renameMap.put(path, extension);
							out.printAnalysisMessage("Found " + StringUtils.stripStart(extension.toUpperCase(Locale.ROOT), ".") + " file with suspicious extension: '" + path + "'.");
						});
					}
				}
				analyzedCount++;
			}
			catch (final MimeTypeException | IOException | RuntimeException e) {
				skippedCount++;
				out.printAnalysisError("Skipping '" + path + "' due to an exception: " + e, e);
			}
		}

		private String detectMediaType(@NonNull final Path path) throws IOException {
			final Metadata metadata = new Metadata();
			try (final InputStream stream = TikaInputStream.get(path, metadata)) {
				closeables.put(path, stream);
				return tika.detect(stream, metadata);
			}
			finally {
				closeables.remove(path);
			}
		}

		private Optional<String> findBetterExtension(@NonNull final Path path, @NonNull final List<String> knownExtensions) { // non-private for test only access
			final String currentExtension = FilenameUtils.getExtension(path.toString());
			final String bestExtension = knownExtensions.get(0);
			if (currentExtension.isEmpty() || knownExtensions.stream().noneMatch(e -> e.equalsIgnoreCase('.' + currentExtension))) {
				return Optional.of(bestExtension);
			}
			else {
				return Optional.empty();
			}
		}

	}

}
