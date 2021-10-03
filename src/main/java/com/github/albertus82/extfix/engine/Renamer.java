package com.github.albertus82.extfix.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;

import com.github.albertus82.extfix.Console;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Renamer {

	@NonNull
	private final Console out;

	private final boolean dryRun;

	@Getter
	private int successCount;

	@Getter
	private int failedCount;

	public void rename(@NonNull final Map<Path, String> map) {
		for (final Entry<Path, String> e : map.entrySet()) {
			rename(e.getKey(), e.getValue());
		}
	}

	private void rename(@NonNull final Path source, @NonNull final String newExtension) {
		final Path target = buildTarget(source, newExtension);
		out.print("Renaming '" + source + "' to '" + target + "'... ");
		try {
			if (!dryRun) {
				Files.move(source, target);
			}
			successCount++;
			out.printLine("Done.");
		}
		catch (final IOException e) {
			failedCount++;
			out.printLine("Failed.");
			out.printError("Cannot rename '" + source + "' due to an exception: " + e, e);
		}
	}

	private static Path buildTarget(@NonNull final Path source, @NonNull final String newExtension) {
		final String oldFileName = source.toString();
		Path target;
		if (FilenameUtils.getExtension(oldFileName).isEmpty()) {
			target = Paths.get(oldFileName + newExtension);
		}
		else {
			target = Paths.get(FilenameUtils.removeExtension(oldFileName) + newExtension);
		}
		int i = 0;
		while (Files.exists(target)) {
			target = Paths.get(FilenameUtils.removeExtension(target.toString()) + " (" + ++i + ")" + newExtension);
		}
		return target;
	}

}
