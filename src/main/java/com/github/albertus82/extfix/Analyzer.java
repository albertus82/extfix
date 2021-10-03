package com.github.albertus82.extfix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeTypeException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Analyzer {

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

	@Getter(value = AccessLevel.PACKAGE) // for test only access
	private final Tika tika = new Tika(tikaConfig);

	@NonNull
	private final Console con;

	@Getter
	private final Map<Path, Path> renames = new TreeMap<>();

	@Getter
	private int count;

	public void analyze(@NonNull final Path path) {
		final Path absolutePath = toAbsolutePath(path);
		try {
			final String mediaType = tika.detect(absolutePath);
			if (mediaType == null) {
				con.printAnalysisMessage("Cannot determine type of '" + absolutePath + "'.");
			}
			else {
				final List<String> exts = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
				log.debug("{} <- {}", exts, absolutePath);
				if (exts.isEmpty()) {
					con.printAnalysisMessage("Cannot determine extension for '" + absolutePath + "'.");
				}
				else {
					fixFileName(absolutePath, exts).ifPresent(fixed -> {
						renames.put(absolutePath, fixed);
						con.printAnalysisMessage("Found " + FilenameUtils.getExtension(fixed.toString()).toUpperCase() + " file with wrong extension: '" + absolutePath + "'.");
					});
				}
			}
			count++;
		}
		catch (final MimeTypeException | IOException | RuntimeException e) {
			con.printAnalysisError("Skipping '" + absolutePath + "' due to an exception: " + e, e);
		}
	}

	static Optional<Path> fixFileName(@NonNull final Path path, @NonNull final List<String> knownExtensions) { // non-private for test only access
		final String currentFileName = path.toString();
		final String currentExtension = FilenameUtils.getExtension(currentFileName);
		final String bestExtension = knownExtensions.get(0);
		if (currentExtension.isEmpty()) {
			return Optional.of(Paths.get(currentFileName + bestExtension));
		}
		else if (knownExtensions.stream().noneMatch(e -> e.equalsIgnoreCase('.' + currentExtension))) {
			return Optional.of(Paths.get(FilenameUtils.removeExtension(currentFileName) + bestExtension));
		}
		else {
			return Optional.empty();
		}
	}

	private static Path toAbsolutePath(@NonNull final Path path) {
		final File file = path.toFile();
		try {
			return file.getCanonicalFile().toPath();
		}
		catch (final IOException e) {
			log.debug("Cannot obtain canonical path, falling back to absolute path:", e);
			return file.getAbsoluteFile().toPath();
		}
	}

}
