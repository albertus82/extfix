package com.github.albertus82.extfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@NoArgsConstructor
@Command(description = "File Extension Fix Tool", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class ExtFix implements Callable<Integer> {

	private final Console con = new Console();

	@Parameters(paramLabel = "<BASE_PATH>", description = "Base directory to scan.")
	private Path basePath;

	@Option(names = { "-n", "--dry-run" }, description = "Do everything except actually rename the files.")
	private boolean dryRun;

	@Option(names = { "-y", "--yes" }, description = "Automatic yes to prompts (run non-interactively).")
	private boolean yes;

	@Option(names = { "-X", "--errors" }, description = "Produce execution error messages.")
	private boolean errors;

	@Option(names = { "-L", "--links" }, description = "Follow links.")
	private boolean links;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Extensions extensions;

	ExtFix(@NonNull final Path basePath, final boolean dryRun, final boolean yes, @NonNull final Extensions extensions) { // for test only access
		this.basePath = basePath;
		this.dryRun = dryRun;
		this.yes = yes;
		this.extensions = extensions;
	}

	public static void main(final String... args) {
		System.exit(new CommandLine(new ExtFix()).setCommandName(BuildInfo.getProperty("project.artifactId")).setOptionsCaseInsensitive(true).execute(args));
	}

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

		final Analyzer analyzer = new Analyzer(con);

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
						analyzer.analyze(path);
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

		});

		con.clearAnalysisLine();
		con.printLine(analyzer.getCount() + " files analyzed.");

		if (!yes) {
			con.print(analyzer.getRenames().size() + " files are about to be renamed. Do you want to continue? [y/N] ");
			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			try {
				final String userAnswer = StringUtils.trimToEmpty(br.readLine());
				final Collection<String> yesAnswers = Arrays.asList("yes", "y");
				if (!yesAnswers.contains(userAnswer.toLowerCase()) && !yesAnswers.contains(userAnswer.toLowerCase(Locale.ROOT))) {
					con.printLine("Abort.");
					return ExitCode.OK; // exit immediately
				}
			}
			catch (final IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		final Renamer renamer = new Renamer(con, dryRun);
		renamer.rename(analyzer.getRenames());
		con.printLine(renamer.getCount() + " files renamed.");

		return ExitCode.OK;
	}

}
