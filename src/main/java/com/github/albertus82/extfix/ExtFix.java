package com.github.albertus82.extfix;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeTypeException;

import lombok.NonNull;
import lombok.extern.java.Log;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Log
@Command(mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class ExtFix implements Callable<Integer> {

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
	final Tika tika = new Tika(tikaConfig);

	@Parameters
	private Path basePath;

	@Option(names = { "-n", "--dry-run" })
	private boolean dryRun;

	public static void main(final String... args) {
		System.exit(new CommandLine(new ExtFix()).setCommandName(BuildInfo.getProperty("project.artifactId")).setOptionsCaseInsensitive(true).execute(args));
	}

	private int count = 0;

	@Override
	public Integer call() throws IOException {
		basePath = basePath.toFile().getCanonicalFile().toPath();
		final Map<String, String> renames = new TreeMap<>();
		final Stream<Path> stream = PathUtils.walk(basePath, CanReadFileFilter.CAN_READ.and(new SuffixFileFilter(getSuffixes(), IOCase.INSENSITIVE)), Short.MAX_VALUE, false, FileVisitOption.FOLLOW_LINKS);
		stream.filter(path -> path.getFileName() != null).forEach(path -> {
			try {
				final File file = path.toFile().getCanonicalFile();
				final String mediaType = tika.detect(file);
				if (mediaType == null) {
					log.log(Level.WARNING, "Cannot determine type of ''{0}''.", file);
				}
				else {
					final List<String> extensions = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
					log.log(Level.FINE, "{0} <- {1}", new Object[] { extensions, file });
					if (extensions.isEmpty()) {
						log.log(Level.WARNING, "Cannot determine file extension for ''{0}''.", file);
					}
					else {
						fixFileName(file.getName(), extensions).ifPresent(newName -> {
							renames.put(file.getName(), newName);
							log.log(Level.FINE, "{0} -> {1}", new Object[] { file, newName });
						});
					}
				}
				count++;
			}
			catch (final MimeTypeException | RuntimeException | IOException e) {
				log.log(Level.WARNING, e, () -> "Skipped '" + path + "':");
			}
		});

		log.log(Level.INFO, "{0} files analyzed.", count);

		for (final Entry<String, String> e : renames.entrySet()) {
			log.log(Level.INFO, "{0} -> {1}", new String[] { e.getKey(), e.getValue() });
			if (!dryRun) {
				Files.move(Path.of(e.getKey()), Path.of(e.getValue()));
			}
		}
		log.log(Level.INFO, "{0} files renamed.", renames.size());

		return ExitCode.OK;
	}

	private static String[] getSuffixes() {
		final String[] suffixes = Extensions.array();
		log.log(Level.INFO, "Extensions: {0}.", Arrays.toString(suffixes));
		return suffixes;
	}

	private static Optional<String> fixFileName(@NonNull final String currentFileName, @NonNull final List<String> knownExtensions) {
		final String currentExtension = FilenameUtils.getExtension(currentFileName);
		final String bestExtension = knownExtensions.get(0);
		if (currentExtension.isEmpty()) {
			return Optional.of(currentFileName + bestExtension);
		}
		else if (knownExtensions.stream().noneMatch(e -> e.equalsIgnoreCase('.' + currentExtension))) {
			return Optional.of(FilenameUtils.removeExtension(currentFileName) + bestExtension);
		}
		else {
			return Optional.empty();
		}
	}

}
