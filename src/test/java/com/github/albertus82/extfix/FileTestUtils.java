package com.github.albertus82.extfix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileTestUtils {

	public static <T extends Throwable> void runWithTempDir(final TempDirExec<T> exec) throws IOException, T {
		final String uuid = UUID.randomUUID().toString().replace("-", "");
		final File tempFile = File.createTempFile(uuid, null);
		final File tempDir = new File(tempFile.getParent() + File.separator + uuid);
		if (!tempFile.delete()) {
			log.warn("Unable to delete temporary file \"{}\".", tempFile);
			tempFile.deleteOnExit();
		}
		tempDir.mkdir();
		log.info("Created temporary directory \"{}\".", tempDir);
		try {
			exec.execute(tempDir.toPath());
		}
		finally {
			try {
				FileUtils.deleteDirectory(tempDir);
				log.info("Deleted temporary directory \"{}\".", tempDir);
			}
			catch (final IOException e) {
				log.warn("Cannot delete temporary directory \"" + tempDir + "\":", e);
			}
		}
	}

	public static Path copyResourceToDir(final String resourceName, final Path destDir) throws IOException {
		final Path target = Paths.get(destDir.toString(), resourceName);
		try (final InputStream in = FileTestUtils.class.getResourceAsStream('/' + resourceName)) {
			Files.copy(in, target);
			log.info("Created file \"{}\".", target);
		}
		return target;
	}

}
