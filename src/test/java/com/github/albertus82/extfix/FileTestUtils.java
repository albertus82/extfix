package com.github.albertus82.extfix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileTestUtils {

	public static <T extends Throwable> void runWithTempDir(final TempDirExec<T> exec) throws IOException, T {
		final String uuid = UUID.randomUUID().toString().replace("-", "");
		final File tempFile = File.createTempFile(uuid, null);
		final File tempDir = new File(tempFile.getParent() + File.separator + uuid);
		if (!tempFile.delete()) {
			log.log(Level.WARNING, "Unable to delete temporary file \"{0}\".", tempFile);
			tempFile.deleteOnExit();
		}
		tempDir.mkdir();
		log.log(Level.INFO, "Created temporary directory \"{0}\".", tempDir);
		try {
			exec.execute(tempDir.toPath());
		}
		finally {
			try {
				FileUtils.deleteDirectory(tempDir);
				log.log(Level.INFO, "Deleted temporary directory \"{0}\".", tempDir);
			}
			catch (final IOException e) {
				log.log(Level.WARNING, "Cannot delete temporary directory \"" + tempDir + "\":", e);
			}
		}
	}

	public static Path copyResourceToDir(final String resourceName, final Path destDir) throws IOException {
		final Path target = Paths.get(destDir.toString(), resourceName);
		try (final InputStream in = FileTestUtils.class.getResourceAsStream('/' + resourceName)) {
			Files.copy(in, target);
			log.log(Level.INFO, "Created temporary file \"{0}\".", target);
		}
		return target;
	}

}
