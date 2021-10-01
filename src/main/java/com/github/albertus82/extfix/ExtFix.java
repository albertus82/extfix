package com.github.albertus82.extfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Log
@Command(description = "File Extension Fix Tool", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class ExtFix implements Callable<Integer> {

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
	final Tika tika = new Tika(tikaConfig);

	@Parameters(paramLabel = "<BASE_PATH>", description = "Base directory to scan.")
	private Path basePath;

	@Option(names = { "-n", "--dry-run" }, description = "Do everything except actually rename the files.")
	private boolean dryRun;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Extensions extensions;

	static class Extensions {

		private final Set<String> set = new TreeSet<>();

		@Option(names = { "-e", "--ext" }, paramLabel = "<EXT>", required = true, description = "File extension to treat.")
		private String[] array;

		@Option(names = { "-f", "--file" }, paramLabel = "<FILE>", required = true, description = "File containing a list of extensions to treat.")
		private Path path;

		public String[] array() {
			if (set.isEmpty()) {
				if (path != null) {
					set.addAll(from(path));
				}
				else {
					set.addAll(from(array));
				}
			}
			return set.toArray(new String[set.size()]);
		}

		private static Set<String> from(@NonNull final String[] array) {
			final Set<String> set = new HashSet<>();
			for (final String item : array) {
				put(item, set);
			}
			return set;
		}

		private static Set<String> from(@NonNull final Path path) {
			final Set<String> set = new HashSet<>();
			try (final BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				String item;
				while ((item = br.readLine()) != null) {
					put(item, set);
				}
				return set;
			}
			catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		private static void put(@NonNull final String item, @NonNull final Set<String> dest) {
			final StringBuilder sb = new StringBuilder(item.trim().toLowerCase(Locale.ROOT));
			if (sb.length() > 0) {
				if (sb.charAt(0) != '.') {
					sb.insert(0, '.');
				}
				dest.add(sb.toString());
			}
		}
	}

	public static void main(final String... args) {
		System.exit(new CommandLine(new ExtFix()).setCommandName(BuildInfo.getProperty("project.artifactId")).setOptionsCaseInsensitive(true).execute(args));
	}

	private int count = 0;

	@Override
	public Integer call() throws IOException {
		basePath = basePath.toFile().getCanonicalFile().toPath();
		log.log(Level.INFO, "Base path: ''{0}''.", basePath);
		final Map<Path, Path> renames = new TreeMap<>();

		final Stream<Path> stream = PathUtils.walk(basePath, CanReadFileFilter.CAN_READ.and(new SuffixFileFilter(getSuffixes(), IOCase.INSENSITIVE)), Short.MAX_VALUE, false, FileVisitOption.FOLLOW_LINKS);
		stream.filter(path -> path.getFileName() != null).forEach(p -> {
			try {
				final Path path = p.toFile().getCanonicalFile().toPath();
				final String mediaType = tika.detect(path);
				if (mediaType == null) {
					log.log(Level.WARNING, "Cannot determine type of ''{0}''.", path);
				}
				else {
					final List<String> extensions = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
					log.log(Level.FINE, "{0} <- {1}", new Object[] { extensions, path });
					if (extensions.isEmpty()) {
						log.log(Level.WARNING, "Cannot determine file extension for ''{0}''.", path);
					}
					else {
						fixFileName(path, extensions).ifPresent(fixed -> {
							renames.put(path, fixed);
							log.log(Level.FINE, "{0} -> {1}", new Path[] { path, fixed });
						});
					}
				}
				count++;
			}
			catch (final MimeTypeException | IOException e) {
				log.log(Level.WARNING, e, () -> "Skipped '" + p + "':");
			}
			catch (final RuntimeException e) {
				log.log(Level.SEVERE, e, () -> "Skipped '" + p + "':");
			}
		});

		log.log(Level.INFO, "{0} files analyzed.", count);

		for (final Entry<Path, Path> e : renames.entrySet()) {
			log.log(Level.INFO, "{0} -> {1}", new Path[] { e.getKey(), e.getValue() });
			if (!dryRun) {
				Files.move(e.getKey(), e.getValue());
			}
		}
		log.log(Level.INFO, "{0} files renamed.", renames.size());

		return ExitCode.OK;
	}

	String[] getSuffixes() {
		final String[] suffixes = extensions.array();
		log.log(Level.INFO, "Extensions: {0}.", Arrays.toString(suffixes));
		return suffixes;
	}

	static Optional<Path> fixFileName(@NonNull final Path path, @NonNull final List<String> knownExtensions) {
		final String currentFileName = path.toString();
		final String currentExtension = FilenameUtils.getExtension(currentFileName);
		final String bestExtension = knownExtensions.get(0);
		if (currentExtension.isEmpty()) {
			return Optional.of(Path.of(currentFileName + bestExtension));
		}
		else if (knownExtensions.stream().noneMatch(e -> e.equalsIgnoreCase('.' + currentExtension))) {
			return Optional.of(Path.of(FilenameUtils.removeExtension(currentFileName) + bestExtension));
		}
		else {
			return Optional.empty();
		}
	}

}
