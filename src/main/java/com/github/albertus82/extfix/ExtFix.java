package com.github.albertus82.extfix;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
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

	private static final String ANALYZING = "Analyzing directory ";

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

	final StringBuilder currentDir = new StringBuilder();

	@Override
	public Integer call() throws IOException {
		basePath = basePath.toFile().getCanonicalFile().toPath();
		System.out.println("Base path: '" + basePath + "'.");

		final List<String> suffixes = extensions.get();
		System.out.println("Extensions: " + suffixes + '.');

		System.out.print(ANALYZING);

		final Map<Path, Path> renames = new TreeMap<>();
		Files.walkFileTree(basePath, links ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : Collections.emptySet(), Short.MAX_VALUE, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
				final StringBuilder sb = new StringBuilder();
				for (int i = 0; i < currentDir.length(); i++) {
					sb.append('\b');
				}
				System.out.print(sb);
				currentDir.setLength(0);
				currentDir.append(StringUtils.abbreviateMiddle(dir.toFile().getCanonicalPath(), "...", 79 - ANALYZING.length()));
				for (int i = ANALYZING.length() + currentDir.length(); i < 79; i++) {
					currentDir.append(' ');
				}
				System.out.print(currentDir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(@NonNull Path path, final BasicFileAttributes attrs) {
				if (FileVisitResult.CONTINUE.equals((new SuffixFileFilter(suffixes, IOCase.INSENSITIVE)).accept(path, attrs))) {
					if (FileVisitResult.CONTINUE.equals(CanReadFileFilter.CAN_READ.accept(path, attrs))) {
						try {
							path = path.toFile().getCanonicalFile().toPath();
							final String mediaType = tika.detect(path);
							if (mediaType == null) {
								printMessage("Cannot determine type of '" + path + "'.");
							}
							else {
								final List<String> exts = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
								log.debug("{} <- {}", exts, path);
								if (exts.isEmpty()) {
									printMessage("Cannot determine extension for '" + path + "'.");
								}
								else {
									final Optional<Path> fixed = fixFileName(path, exts);
									if (fixed.isPresent()) {
										renames.put(path, fixed.get());
										printMessage("Found " + FilenameUtils.getExtension(fixed.get().toString()).toUpperCase() + " file with wrong extension: '" + path + "'.");
									}
								}
							}
							count++;
						}
						catch (final MimeTypeException | IOException | RuntimeException e) {
							printError("Skipping '" + path + "' due to an exception: " + e, e);
						}
					}
					else {
						printMessage("Skipping not readable file '" + path + "'.");
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(final Path file, final IOException e) {
				printError("Skipping '" + file + "' due to an exception: " + e, e);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException e) {
				return FileVisitResult.CONTINUE;
			}

			private void printMessage(@NonNull final String message) {
				clearConsoleLine();
				System.out.println(message);
				System.out.print(ANALYZING);
			}

			private void printError(@NonNull final String message, @NonNull final Throwable e) {
				clearConsoleLine();
				if (errors) {
					e.printStackTrace();
				}
				System.out.println(message);
				System.out.print(ANALYZING);
			}
		});

		clearConsoleLine();
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

	private void clearConsoleLine() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < currentDir.length() + ANALYZING.length(); i++) {
			sb.append('\b');
		}
		System.out.print(sb);
	}

}
