package com.github.albertus82.extfix;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;
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
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Slf4j
@NoArgsConstructor
@Command(description = "File Extension Fix Tool", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
@SuppressWarnings("java:S106") // Replace this use of System.out or System.err by a logger. Standard outputs should not be used directly to log anything (java:S106)
public class ExtFix implements Callable<Integer> {

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

	@Getter(value = AccessLevel.PACKAGE) // for test only access
	private final Tika tika = new Tika(tikaConfig);

	@Parameters(paramLabel = "<BASE_PATH>", description = "Base directory to scan.")
	private Path basePath;

	@Option(names = { "-n", "--dry-run" }, description = "Do everything except actually rename the files.")
	private boolean dryRun;

	@Option(names = { "-X", "--errors" }, description = "Produce execution error messages.")
	private boolean errors;

	@Option(names = { "-L", "--links" }, description = "Follow links.")
	private boolean links;

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
		System.out.println("Base path: '" + basePath + "'.");

		final List<String> suffixes = extensions.get();
		System.out.println("Extensions: " + suffixes + '.');

		final List<FileVisitOption> option = new ArrayList<>();
		if (links) {
			option.add(FileVisitOption.FOLLOW_LINKS);
		}

		final Stream<Path> stream = PathUtils.walk(basePath, CanReadFileFilter.CAN_READ.and(new SuffixFileFilter(suffixes, IOCase.INSENSITIVE)), Short.MAX_VALUE, false, option.toArray(new FileVisitOption[option.size()]));

		System.out.print("Analyzing... ");
		System.out.print(getWaitChar());

		final Map<Path, Path> renames = new TreeMap<>();
		stream.filter(path -> path.getFileName() != null).forEach(p -> {
			try {
				final Path path = p.toFile().getCanonicalFile().toPath();
				final String mediaType = tika.detect(path);
				if (mediaType == null) {
					System.out.println();
					System.out.println("Cannot determine type of '" + path + "'.");
					System.out.print("Analyzing... ");
				}
				else {
					final List<String> exts = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
					log.debug("{} <- {}", exts, path);
					if (exts.isEmpty()) {
						System.out.println();
						System.out.println("Cannot determine extension for '" + path + "'.");
						System.out.print("Analyzing... ");
					}
					else {
						final Optional<Path> fixed = fixFileName(path, exts);
						if (fixed.isPresent()) {
							renames.put(path, fixed.get());
							System.out.println();
							System.out.println("Found " + FilenameUtils.getExtension(fixed.get().toString()).toUpperCase() + " file with wrong extension: '" + path + "'.");
							System.out.print("Analyzing... ");
						}
						else {
							System.out.print('\b');
						}
					}
				}
				count++;
			}
			catch (final MimeTypeException | IOException | RuntimeException e) {
				System.out.println();
				if (errors) {
					e.printStackTrace();
				}
				System.out.println("Skipped '" + p + "': " + e);
				System.out.print("Analyzing... ");
			}
			System.out.print(getWaitChar());
		});

		System.out.println();
		System.out.println(count + " files analyzed.");

		for (final Entry<Path, Path> e : renames.entrySet()) {
			rename(e.getKey(), e.getValue());
		}
		System.out.println(renames.size() + " files renamed.");

		return ExitCode.OK;
	}

	Path rename(@NonNull final Path source, @NonNull Path target) throws IOException { // non-private for test only access
		int i = 0;
		while (Files.exists(target)) {
			target = Paths.get(FilenameUtils.removeExtension(target.toString()) + " (" + ++i + ")." + FilenameUtils.getExtension(target.toString()));
		}
		System.out.println("Renaming '" + source + "' to '" + target + "'.");
		if (!dryRun) {
			Files.move(source, target);
		}
		return target;
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

	private char getWaitChar() {
		switch (count % 4) {
		case 0:
			return '|';
		case 1:
			return '/';
		case 2:
			return '-';
		default:
			return '\\';
		}
	}

}
