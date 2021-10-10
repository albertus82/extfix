package com.github.albertus82.extfix.engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

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
	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	public class RenameResult {
		int renamedCount;
		int errorCount;
		int skippedCount;
	}

	public RenameResult rename(@NonNull final Map<Path, String> renameMap, final boolean dryRun, final boolean yes) {
		int renamedCount = 0;
		int errorCount = 0;
		int skippedCount = 0;
		boolean all = false;
		for (final Entry<Path, String> entry : renameMap.entrySet()) {
			final Path source = entry.getKey();
			final Path target = buildTarget(source, entry.getValue());
			if (!yes && !all) {
				final Answer userAnswer = confirm(source, target);
				if (Answer.ALL.equals(userAnswer)) {
					all = true;
				}
				else if (Answer.NO.equals(userAnswer)) {
					con.getOut().println("Skipping '" + source + "'.");
					skippedCount++;
					continue;
				}
				else if (Answer.CANCEL.equals(userAnswer)) {
					skippedCount = renameMap.size() - renamedCount - errorCount;
					break;
				}
				else if (!Answer.YES.equals(userAnswer)) {
					throw new IllegalStateException(String.valueOf(userAnswer));
				}
			}
			if (rename(source, target, dryRun)) {
				renamedCount++;
			}
			else {
				errorCount++;
			}
		}
		return new RenameResult(renamedCount, errorCount, skippedCount);
	}

	private enum Answer {
		YES("yes", "y"),
		NO("no", "n"),
		ALL("all", "a"),
		CANCEL("cancel", "c");

		private final Set<String> answers = new HashSet<>();

		private Answer(@NonNull final String... answers) {
			Arrays.stream(answers).forEach(a -> {
				if (a == null || a.trim().isEmpty()) {
					throw new IllegalArgumentException(a);
				}
				this.answers.add(a.trim().toLowerCase());
				this.answers.add(a.trim().toLowerCase(Locale.ROOT));
			});
		}

		private static Optional<Answer> forInput(final String input) {
			if (input != null) {
				for (final Answer a : Answer.values()) {
					if (a.answers.contains(input.trim().toLowerCase()) || a.answers.contains(input.trim().toLowerCase(Locale.ROOT))) {
						return Optional.of(a);
					}
				}
			}
			return Optional.empty();
		}
	}

	private Answer confirm(@NonNull final Path source, @NonNull final Path target) {
		con.getOut().print("Rename '" + source + "' to '" + target.getFileName() + "'? [y(es)/N(o)/a(ll)/c(ancel)] ");
		final BufferedReader br = new BufferedReader(new InputStreamReader(con.getIn()));
		try {
			return Answer.forInput(br.readLine()).orElse(Answer.NO);
		}
		catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
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
			e.printStackTrace(con.getErr());
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

}
