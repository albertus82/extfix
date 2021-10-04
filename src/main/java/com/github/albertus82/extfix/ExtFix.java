package com.github.albertus82.extfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.github.albertus82.extfix.engine.Analyzer;
import com.github.albertus82.extfix.engine.Renamer;
import com.github.albertus82.extfix.util.PathUtils;

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

	private final Console out = new Console();

	@NonNull
	@Parameters(paramLabel = "<PATH>", description = "Directory to scan for files with invalid extension.")
	private Path path;

	@NonNull
	@ArgGroup(exclusive = true)
	private Extensions extensions = new Extensions();

	@Option(names = { "-L", "--links" }, description = "Follow links.")
	private boolean links;

	@Option(names = { "-n", "--dry-run" }, description = "Do everything except actually rename the files.")
	private boolean dryRun;

	@Option(names = { "-R", "--recursive" }, description = "Operate on files and directories recursively.")
	private boolean recursive;

	@Option(names = { "-X", "--errors" }, description = "Produce execution error messages.")
	private boolean errors;

	@Option(names = { "-y", "--yes" }, description = "Automatic yes to prompts (run non-interactively).")
	private boolean yes;

	ExtFix(@NonNull Path path, @NonNull Extensions extensions, boolean links, boolean dryRun, boolean errors, boolean yes) { // for test only access
		this.path = path;
		this.extensions = extensions;
		this.links = links;
		this.dryRun = dryRun;
		this.errors = errors;
		this.yes = yes;
	}

	public static void main(final String... args) {
		System.exit(new CommandLine(new ExtFix()).setCommandName(BuildInfo.getProperty("project.artifactId")).setOptionsCaseInsensitive(true).execute(args));
	}

	@Override
	public Integer call() throws IOException {
		if (errors) {
			out.setStackTraces(true);
			out.printLine("Error stack traces are turned on.");
		}

		path = PathUtils.absolute(path);
		out.printLine("Path: '" + path + "'.");

		final Analyzer analyzer = new Analyzer(out, extensions.get());

		if (recursive) {
			Files.walkFileTree(path, links ? EnumSet.of(FileVisitOption.FOLLOW_LINKS) : Collections.emptySet(), Short.MAX_VALUE, analyzer);
		}
		else {
			try (final Stream<Path> stream = Files.list(path)) {
				stream.forEach(entry -> {
					try {
						final BasicFileAttributes attrs = links ? Files.readAttributes(entry, BasicFileAttributes.class) : Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
						analyzer.visitFile(entry, attrs);
					}
					catch (final IOException e) {
						analyzer.visitFileFailed(entry, e);
					}
				});
			}
		}

		out.clearAnalysisLine();
		out.printLine(analyzer.getAnalyzedCount() + " files analyzed (" + analyzer.getSkippedCount() + " elements skipped).");

		if (analyzer.getResults().isEmpty()) {
			out.printLine("No problems detected.");
			return ExitCode.OK; // exit immediately
		}

		if (!yes) {
			out.print(analyzer.getResults().size() + " files are about to be renamed. Do you want to continue? [y/N] ");
			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			final String userAnswer = StringUtils.trimToEmpty(br.readLine());
			final Collection<String> yesAnswers = Arrays.asList("yes", "y");
			if (!yesAnswers.contains(userAnswer.toLowerCase()) && !yesAnswers.contains(userAnswer.toLowerCase(Locale.ROOT))) {
				out.printLine("Abort.");
				return ExitCode.OK; // exit immediately
			}
		}

		final Renamer renamer = new Renamer(out, dryRun);
		renamer.rename(analyzer.getResults());
		out.printLine(renamer.getSuccessCount() + " files renamed (" + renamer.getFailedCount() + " failed).");
		return ExitCode.OK;
	}

}
