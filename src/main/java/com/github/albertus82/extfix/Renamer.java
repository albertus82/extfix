package com.github.albertus82.extfix;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class Renamer {

	@NonNull
	private final Console con;

	private final boolean dryRun;

	@Getter
	private int count;

	public void rename(@NonNull Map<Path, Path> map) {
		for (final Entry<Path, Path> e : map.entrySet()) {
			rename(e.getKey(), e.getValue());
		}
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
			count++;
			con.printLine("Done.");
			return Optional.of(target);
		}
		catch (final IOException e) {
			con.printLine("Failed.");
			con.printError("Cannot rename '" + source + "' due to an exception: " + e, e);
			return Optional.empty();
		}
	}

}
