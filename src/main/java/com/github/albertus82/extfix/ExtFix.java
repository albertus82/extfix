package com.github.albertus82.extfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;

import com.github.albertus82.extfix.engine.Analyzer;
import com.github.albertus82.extfix.engine.Analyzer.AnalysisResult;
import com.github.albertus82.extfix.engine.Renamer;
import com.github.albertus82.extfix.engine.Renamer.RenameResult;
import com.github.albertus82.extfix.util.PathUtils;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE) // for test only access
@Command(description = "File Extension Fix Tool", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
public class ExtFix implements Callable<Integer> {

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

	public static void main(final String... args) {
		System.exit(new CommandLine(new ExtFix()).setCommandName(BuildInfo.getProperty("project.artifactId")).setOptionsCaseInsensitive(true).execute(args));
	}

	@Override
	public Integer call() throws IOException {
		final Console con = new Console(errors);
		if (errors) {
			con.getOut().println("Error stack traces are turned on.");
		}

		path = PathUtils.absolute(path);

		final Set<String> flags = new TreeSet<>();
		if (links) {
			flags.add("follow links");
		}
		if (recursive) {
			flags.add("recursive");
		}
		final StringBuilder sb = new StringBuilder("Path: '").append(path).append("'");
		if (flags.isEmpty()) {
			sb.append('.');
		}
		else {
			sb.append(" (").append(StringUtils.join(flags, ", ")).append(").");
		}
		con.getOut().println(sb);

		final AnalysisResult analysisResult = new Analyzer(con).analyze(path, links, recursive, extensions.get());
		con.getOut().println(analysisResult.getAnalyzedCount() + " files analyzed (" + analysisResult.getSkippedCount() + " elements skipped).");

		if (analysisResult.getRenameMap().isEmpty()) {
			con.getOut().println("No problems detected.");
			return ExitCode.OK; // exit immediately
		}

		if (!yes) {
			con.getOut().print(analysisResult.getRenameMap().size() + " files are about to be renamed. Do you want to continue? [y/N] ");
			final BufferedReader br = new BufferedReader(new InputStreamReader(con.getIn()));
			final String userAnswer = StringUtils.trimToEmpty(br.readLine());
			final Collection<String> yesAnswers = Arrays.asList("yes", "y");
			if (!yesAnswers.contains(userAnswer.toLowerCase()) && !yesAnswers.contains(userAnswer.toLowerCase(Locale.ROOT))) {
				con.getOut().println("Abort.");
				return ExitCode.OK; // exit immediately
			}
		}

		final RenameResult renameResult = new Renamer(con).rename(analysisResult.getRenameMap(), dryRun);
		con.getOut().println(renameResult.getSuccessCount() + " files renamed (" + renameResult.getFailedCount() + " failed).");
		return ExitCode.OK;
	}

}
