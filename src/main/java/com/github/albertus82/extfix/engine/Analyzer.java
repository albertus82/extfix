package com.github.albertus82.extfix.engine;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.file.PathFilter;
import org.apache.commons.io.file.PathVisitor;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeTypeException;

import com.github.albertus82.extfix.Console;
import com.github.albertus82.extfix.util.PathUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Analyzer implements PathVisitor {

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

	@Getter(value = AccessLevel.PACKAGE) // for test only access
	private final Tika tika = new Tika(tikaConfig);

	@NonNull
	private final Console out;

	@NonNull
	private final PathFilter pathFilter;

	@Getter
	private final Map<Path, String> results = new TreeMap<>();

	@Getter
	private int analyzedCount;

	@Getter
	private int skippedCount;

	public Analyzer(@NonNull final Console out, final String... suffixes) {
		this.out = out;
		if (suffixes != null && suffixes.length > 0) {
			this.pathFilter = new SuffixFileFilter(suffixes, IOCase.INSENSITIVE);
			out.printLine("Extensions: " + Arrays.toString(suffixes) + '.');
		}
		else {
			this.pathFilter = (p, a) -> FileVisitResult.CONTINUE;
		}
	}

	@Override
	public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
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
			final String mediaType = tika.detect(path);
			if (mediaType == null) {
				out.printAnalysisMessage("Cannot determine type of '" + path + "'.");
			}
			else {
				final List<String> exts = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
				log.debug("{} <- {}", exts, path);
				if (exts.isEmpty()) {
					out.printAnalysisMessage("Cannot determine extension for '" + path + "'.");
				}
				else {
					findBetterExtension(path, exts).ifPresent(ext -> {
						results.put(path, ext);
						out.printAnalysisMessage("Found " + ext.toUpperCase() + " file with unexpected extension: '" + path + "'.");
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

	static Optional<String> findBetterExtension(@NonNull final Path path, @NonNull final List<String> knownExtensions) { // non-private for test only access
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
