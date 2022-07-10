package io.github.albertus82.extfix.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

import io.github.albertus82.extfix.Console;
import io.github.albertus82.extfix.util.PathUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Analyzer {

	private static final String ANALYSIS_PREFIX = "Analyzing directory ";

	private static final Map<Path, AutoCloseable> closeables = new ConcurrentHashMap<>();

	static {
		final Thread shutdownHook = new Thread(() -> {
			for (final Entry<Path, AutoCloseable> entry : closeables.entrySet()) {
				try {
					entry.getValue().close();
				}
				catch (final Exception e) {
					log.debug("Cannot close '" + entry.getKey() + "':", e);
				}
			}
		});
		shutdownHook.setPriority(Thread.MAX_PRIORITY);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
	}

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
	private final Tika tika = new Tika(tikaConfig);

	@NonNull
	private final Console con;

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	public class AnalysisResult {
		@NonNull
		Map<Path, String> renameMap;

		int analyzedCount;
		int skippedCount;
	}

	public AnalysisResult analyze(@NonNull final Path path, final boolean links, final boolean recursive, final String... extensions) throws IOException {
		final List<String> suffixes = normalizeExtensions(extensions);
		final Visitor visitor = new Visitor(buildPathFilter(suffixes));
		if (recursive) {
			Files.walkFileTree(path, links ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : Collections.emptySet(), Short.MAX_VALUE, visitor);
		}
		else {
			visitor.preVisitDirectory(path, null);
			try (final Stream<Path> stream = Files.list(path)) {
				closeables.put(path, stream);
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
			finally {
				closeables.remove(path);
			}
		}
		clearAnalysisLine(visitor.getPrintedDirectory());
		return new AnalysisResult(Collections.unmodifiableMap(visitor.getRenameMap()), visitor.getAnalyzedCount(), visitor.getSkippedCount());
	}

	private PathFilter buildPathFilter(final List<String> suffixes) {
		if (suffixes != null && !suffixes.isEmpty()) {
			con.getOut().println("Extensions: " + suffixes + '.');
			return new SuffixFileFilter(suffixes, IOCase.INSENSITIVE);
		}
		else {
			con.getOut().println("Extensions: all.");
			return (p, a) -> FileVisitResult.CONTINUE;
		}
	}

	private static List<String> normalizeExtensions(final String[] extensions) {
		final Collection<String> set = new TreeSet<>();
		if (extensions != null && extensions.length > 0) {
			for (final String extension : extensions) {
				final StringBuilder suffix = new StringBuilder(extension.trim().toLowerCase(Locale.ROOT));
				if (suffix.length() > 0) {
					if (suffix.charAt(0) != '.') {
						suffix.insert(0, '.');
					}
					set.add(suffix.toString());
				}
			}
		}
		return new ArrayList<>(set);
	}

	@RequiredArgsConstructor
	private class Visitor implements PathVisitor {

		private final Set<String> exclusions = Stream.of("System Volume Information", "$Recycle.Bin").map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());

		@NonNull
		private final PathFilter pathFilter;

		@Getter
		private final Map<Path, String> renameMap = new TreeMap<>();

		@Getter
		private int analyzedCount;

		@Getter
		private int skippedCount;

		@Getter
		private String printedDirectory;

		@Override
		public FileVisitResult preVisitDirectory(@NonNull final Path dir, final BasicFileAttributes attrs) {
			printAnalysisProgress(dir);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(@NonNull final Path path, final BasicFileAttributes attrs) {
			if (FileVisitResult.CONTINUE.equals(pathFilter.accept(path, attrs)) && FileVisitResult.CONTINUE.equals(FileFileFilter.INSTANCE.accept(path, attrs))) {
				if (StreamSupport.stream(path.spliterator(), true).anyMatch(part -> exclusions.contains(part.toString().toUpperCase(Locale.ROOT)))) {
					skippedCount++;
					printAnalysisMessage("Skipping excluded file or path '" + path + "'.");
				}
				else if (!FileVisitResult.CONTINUE.equals(CanReadFileFilter.CAN_READ.accept(path, attrs))) {
					skippedCount++;
					printAnalysisMessage("Skipping not readable file '" + path + "'.");
				}
				else {
					analyze(PathUtils.absolute(path));
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
			printAnalysisError("Skipping '" + file + "' due to an exception: " + e, e);
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
					printAnalysisMessage("Cannot detect media type of '" + path + "'.");
				}
				else {
					final List<String> knownExtensions = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
					log.debug("{} <- {}", knownExtensions, path);
					if (knownExtensions.isEmpty()) {
						printAnalysisMessage("Cannot determine extension for '" + path + "'.");
					}
					else {
						findBetterExtension(path, knownExtensions).ifPresent(extension -> {
							renameMap.put(path, extension);
							if (PathUtils.hasExtension(path)) {
								printAnalysisMessage("Found " + StringUtils.stripStart(extension.toUpperCase(Locale.ROOT), ".") + " file with suspicious extension: '" + path + "'.");
							}
							else {
								printAnalysisMessage("Found " + StringUtils.stripStart(extension.toUpperCase(Locale.ROOT), ".") + " file without extension: '" + path + "'.");
							}
						});
					}
				}
				analyzedCount++;
			}
			catch (final MimeTypeException | IOException | RuntimeException e) {
				skippedCount++;
				printAnalysisError("Skipping '" + path + "' due to an exception: " + e, e);
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

		private Optional<String> findBetterExtension(@NonNull final Path path, @NonNull final List<String> knownExtensions) {
			final String currentExtension = FilenameUtils.getExtension(path.toString());
			final String bestExtension = knownExtensions.get(0);
			if (currentExtension.isEmpty() || knownExtensions.stream().noneMatch(e -> e.equalsIgnoreCase('.' + currentExtension))) {
				return Optional.of(bestExtension);
			}
			else {
				return Optional.empty();
			}
		}

		private void printAnalysisProgress(@NonNull final Path path) {
			final int limit = con.getWidth() - 1;
			final StringBuilder sb = new StringBuilder();
			if (printedDirectory == null) {
				sb.append(ANALYSIS_PREFIX);
			}
			else {
				for (int i = 0; i < printedDirectory.length(); i++) {
					sb.append('\b');
				}
			}
			final String pathString = PathUtils.absolute(path).toString();
			printedDirectory = new String(StringUtils.abbreviateMiddle(pathString, "...", limit - ANALYSIS_PREFIX.length()).getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1);
			sb.append(printedDirectory);
			for (int i = ANALYSIS_PREFIX.length() + printedDirectory.length(); i < limit; i++) {
				sb.append(' ');
			}
			for (int i = ANALYSIS_PREFIX.length() + printedDirectory.length(); i < limit; i++) {
				sb.append('\b');
			}
			con.getOut().print(sb);
		}

		private synchronized void printAnalysisMessage(final String message) {
			clearAnalysisLine(printedDirectory);
			con.getOut().println(message);
			if (printedDirectory != null) {
				con.getOut().print(ANALYSIS_PREFIX + printedDirectory);
			}
		}

		private synchronized void printAnalysisError(final String message, final Throwable e) {
			clearAnalysisLine(printedDirectory);
			if (con.isStackTraces() && e != null) {
				e.printStackTrace(con.getErr());
			}
			con.getOut().println(message);
			if (printedDirectory != null) {
				con.getOut().print(ANALYSIS_PREFIX + printedDirectory);
			}
		}

	}

	private void clearAnalysisLine(final String printedDirectory) {
		if (printedDirectory != null) {
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < ANALYSIS_PREFIX.length() + printedDirectory.length(); i++) {
				sb.append("\b \b"); // replace non-whitespace characters with whitespace
			}
			con.getOut().print(sb);
		}
	}

}
