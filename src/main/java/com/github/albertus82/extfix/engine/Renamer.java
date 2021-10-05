package com.github.albertus82.extfix.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.albertus82.extfix.Console;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class Renamer {

	@NonNull
	private final Console con;

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public class RenameResult {
		int renamedCount;
		int errorCount;
		int skippedCount;
	}

	public RenameResult rename(@NonNull final Map<Path, String> map, final boolean dryRun, final boolean yes) throws IOException {
		int renamedCount = 0;
		int errorCount = 0;
		int skippedCount = 0;
		boolean all = false;
		for (final Entry<Path, String> entry : map.entrySet()) {
			final Path source = entry.getKey();
			final Path target = buildTarget(source, entry.getValue());
			if (yes || all) {
				if (rename(source, target, dryRun)) {
					renamedCount++;
				}
				else {
					errorCount++;
				}
			}
			else {
				con.getOut().print("Rename '" + source + "' to '" + target.getFileName() + "'? [y(es)/N(o)/a(ll)/c(ancel)] ");
				final BufferedReader br = new BufferedReader(new InputStreamReader(con.getIn()));
				final String userAnswer = StringUtils.trimToEmpty(br.readLine());
				final Collection<String> proceedAnswers = Arrays.asList("yes", "y", "all", "a");
				final Collection<String> allAnswers = Arrays.asList("all", "a");
				final Collection<String> cancelAnswers = Arrays.asList("cancel", "c");
				if (checkAnswer(allAnswers, userAnswer)) {
					all = true;
				}
				if (checkAnswer(proceedAnswers, userAnswer)) {
					if (rename(source, target, dryRun)) {
						renamedCount++;
					}
					else {
						errorCount++;
					}
				}
				else if (checkAnswer(cancelAnswers, userAnswer)) {
					skippedCount = map.size() - renamedCount - errorCount;
					break;
				}
				else { // No (default)
					con.getOut().println("Skipping '" + source + "'.");
					skippedCount++;
				}
			}
		}
		return new RenameResult(renamedCount, errorCount, skippedCount);

	}

	private boolean rename(@NonNull final Path source, @NonNull final Path target, final boolean dryRun) {
		con.getOut().print("Renaming '" + source + "' to '" + target.getFileName() + "'... ");
		try {
			if (!dryRun) {
				Files.move(source, target);
			}
			con.getOut().println("Done.");
			return true;
		}
		catch (final IOException | RuntimeException e) {
			con.getOut().println("Failed.");
			printError("Cannot rename '" + source + "' due to an exception: " + e, e);
			return false;
		}
	}

	private void printError(final String message, final Throwable e) {
		if (con.isStackTraces() && e != null) {
			e.printStackTrace();
		}
		con.getOut().println(message);
	}

	private static Path buildTarget(@NonNull final Path source, @NonNull final String newExtension) {
		final String oldFileName = source.toString();
		final Path target;
		if (FilenameUtils.getExtension(oldFileName).isEmpty()) {
			target = Paths.get(oldFileName + newExtension);
		}
		else {
			target = Paths.get(FilenameUtils.removeExtension(oldFileName) + newExtension);
		}
		int i = 0;
		Path availableTarget = target;
		while (Files.exists(availableTarget)) {
			availableTarget = Paths.get(FilenameUtils.removeExtension(target.toString()) + " (" + ++i + ")" + newExtension);
		}
		return availableTarget;
	}

	private static boolean checkAnswer(@NonNull final Collection<String> expected, @NonNull final String actual) {
		return expected.contains(actual.toLowerCase()) || expected.contains(actual.toLowerCase(Locale.ROOT));
	}

}
