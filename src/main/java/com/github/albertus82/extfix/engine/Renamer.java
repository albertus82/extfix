package com.github.albertus82.extfix.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;

import com.github.albertus82.extfix.Console;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@RequiredArgsConstructor
public class Renamer {

	@NonNull
	private final Console out;

	@Value
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public class RenameResult {
		int successCount;
		int failedCount;
	}

	public RenameResult rename(@NonNull final Map<Path, String> map, final boolean dryRun) {
		int successCount = 0;
		int failedCount = 0;
		for (final Entry<Path, String> e : map.entrySet()) {
			if (rename(e.getKey(), e.getValue(), dryRun)) {
				successCount++;
			}
			else {
				failedCount++;
			}
		}
		return new RenameResult(successCount, failedCount);
	}

	private boolean rename(@NonNull final Path source, @NonNull final String newExtension, final boolean dryRun) {
		final Path target = buildTarget(source, newExtension);
		out.print("Renaming '" + source + "' to '" + target + "'... ");
		try {
			if (!dryRun) {
				Files.move(source, target);
			}
			out.printLine("Done.");
			return true;
		}
		catch (final IOException e) {
			out.printLine("Failed.");
			out.printError("Cannot rename '" + source + "' due to an exception: " + e, e);
			return false;
		}
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
