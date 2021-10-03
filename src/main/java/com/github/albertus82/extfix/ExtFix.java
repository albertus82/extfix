package com.github.albertus82.extfix;

import java.io.File;
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
public class ExtFix implements Callable<Integer> {

	private final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();

	@Getter(value = AccessLevel.PACKAGE) // for test only access
	private final Tika tika = new Tika(tikaConfig);

	private final Console con = new Console();

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

	private volatile int analyzedCount = 0;

	@Override
	public Integer call() throws IOException {
		if (errors) {
			con.setStackTraces(true);
			con.printLine("Error stack traces are turned on.");
		}

		basePath = basePath.toFile().getCanonicalFile().toPath();
		con.printLine("Base path: '" + basePath + "'.");

		final List<String> suffixes = extensions.get();
		con.printLine("Extensions: " + suffixes + '.');

		final Map<Path, Path> renames = new TreeMap<>();
		Files.walkFileTree(basePath, links ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : Collections.emptySet(), Short.MAX_VALUE, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
				con.printAnalysisProgress(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(@NonNull Path path, final BasicFileAttributes attrs) {
				if (FileVisitResult.CONTINUE.equals((new SuffixFileFilter(suffixes, IOCase.INSENSITIVE)).accept(path, attrs))) {
					if (FileVisitResult.CONTINUE.equals(CanReadFileFilter.CAN_READ.accept(path, attrs))) {
						analyze(toAbsolutePath(path));
					}
					else {
						con.printAnalysisMessage("Skipping not readable file '" + path + "'.");
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(final Path file, final IOException e) {
				con.printAnalysisError("Skipping '" + file + "' due to an exception: " + e, e);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException e) {
				return FileVisitResult.CONTINUE;
			}

			private void analyze(@NonNull final Path path) {
				try {
					final String mediaType = tika.detect(path);
					if (mediaType == null) {
						con.printAnalysisMessage("Cannot determine type of '" + path + "'.");
					}
					else {
						final List<String> exts = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
						log.debug("{} <- {}", exts, path);
						if (exts.isEmpty()) {
							con.printAnalysisMessage("Cannot determine extension for '" + path + "'.");
						}
						else {
							fixFileName(path, exts).ifPresent(fixed -> {
								renames.put(path, fixed);
								con.printAnalysisMessage("Found " + FilenameUtils.getExtension(fixed.toString()).toUpperCase() + " file with wrong extension: '" + path + "'.");
							});
						}
					}
					analyzedCount++;
				}
				catch (final MimeTypeException | IOException | RuntimeException e) {
					con.printAnalysisError("Skipping '" + path + "' due to an exception: " + e, e);
				}
			}
		});

		con.clearAnalysisLine();
		con.printLine(analyzedCount + " files analyzed.");

		int renamedCount = 0;
		for (final Entry<Path, Path> e : renames.entrySet()) {
			if (rename(e.getKey(), e.getValue()).isPresent()) {
				renamedCount++;
			}
		}
		con.printLine(renamedCount + " files renamed.");

		return ExitCode.OK;
	}

	Optional<Path> rename(@NonNull final Path source, @NonNull Path target) { // non-private for test only access
		int i = 0;
		while (Files.exists(target)) {
			target = Paths.get(FilenameUtils.removeExtension(target.toString()) + " (" + ++i + ")." + FilenameUtils.getExtension(target.toString()));
		}
		con.print("Renaming '" + source + "' to '" + target + "'... ");
		try {
			if (!dryRun) {
				Files.move(source, target);
			}
			con.printLine("Done.");
			return Optional.of(target);
		}
		catch (final IOException e) {
			con.printLine("Failed.");
			con.printError("Cannot rename '" + source + "' due to an exception: " + e, e);
			return Optional.empty();
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
			log.debug("Cannot obtain canonical pathname:", e);
			return file.getAbsoluteFile().toPath();
		}
	}

}
