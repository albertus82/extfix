package com.github.albertus82.extfix.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PathUtils {

	public static Path absolute(@NonNull final Path path) {
		final File file = path.toFile();
		try {
			return file.getCanonicalFile().toPath();
		}
		catch (final IOException e) {
			log.debug("Cannot obtain canonical path for '" + path + "', falling back to absolute path:", e);
			return file.getAbsoluteFile().toPath();
		}
	}

	public static boolean hasExtension(@NonNull final Path path) {
		final String fileName = path.getFileName().toString();
		return !FilenameUtils.getExtension(fileName).isEmpty() && fileName.lastIndexOf('.') != 0;
	}

}
