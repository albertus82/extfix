package com.github.albertus82.extfix;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Log
@NoArgsConstructor
@Command(description = "File Extension Fix Tool", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class ExtFix implements Callable<Integer> {

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

	@Getter(value = AccessLevel.PACKAGE) // for test only access
	private final Tika tika = new Tika(tikaConfig);

	@Parameters(paramLabel = "<BASE_PATH>", description = "Base directory to scan.")
	private Path basePath;

	@Option(names = { "-n", "--dry-run" }, description = "Do everything except actually rename the files.")
	private boolean dryRun;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Extensions extensions;

	ExtFix(@NonNull final Path basePath, final boolean dryRun, @NonNull final Extensions extensions) { // for test only access
		this.basePath = basePath;
		this.dryRun = dryRun;
		this.extensions = extensions;
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

		final Stream<Path> stream = PathUtils.walk(basePath, CanReadFileFilter.CAN_READ.and(new SuffixFileFilter(extensions.array(), IOCase.INSENSITIVE)), Short.MAX_VALUE, false, FileVisitOption.FOLLOW_LINKS);
		stream.filter(path -> path.getFileName() != null).forEach(p -> {
			try {
				final Path path = p.toFile().getCanonicalFile().toPath();
				final String mediaType = tika.detect(path);
				if (mediaType == null) {
					log.log(Level.WARNING, "Cannot determine type of ''{0}''.", path);
				}
				else {
					final List<String> exts = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
					log.log(Level.FINE, "{0} <- {1}", new Object[] { exts, path });
					if (exts.isEmpty()) {
						log.log(Level.WARNING, "Cannot determine file extension for ''{0}''.", path);
					}
					else {
						fixFileName(path, exts).ifPresent(fixed -> {
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
			Path target = e.getValue();
			int i = 0;
			while (Files.exists(target)) {
				target = Path.of(FilenameUtils.removeExtension(target.toString()) + " (" + ++i + ")." + FilenameUtils.getExtension(target.toString()));
			}
			log.log(Level.INFO, "{0} -> {1}", new Path[] { e.getKey(), target });
			if (!dryRun) {
				Files.move(e.getKey(), target);
			}
		}
		log.log(Level.INFO, "{0} files renamed.", renames.size());

		return ExitCode.OK;
	}

	static Optional<Path> fixFileName(@NonNull final Path path, @NonNull final List<String> knownExtensions) { // non-private for test only access
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
