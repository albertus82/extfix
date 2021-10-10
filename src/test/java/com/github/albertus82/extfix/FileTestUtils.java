package com.github.albertus82.extfix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.function.ThrowingConsumer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileTestUtils {

	public static void runWithTempDir(@NonNull final ThrowingConsumer<Path> consumer) throws Throwable {
		final String uuid = UUID.randomUUID().toString().replace("-", "");
		final File tempFile = File.createTempFile(uuid, null);
		File tempDir = new File(tempFile.getParent() + File.separator + uuid);
		if (!tempFile.delete()) {
			log.warn("Unable to delete temporary file \"{}\".", tempFile);
			tempFile.deleteOnExit();
		}
		if (tempDir.mkdir()) {
			tempDir = tempDir.getCanonicalFile();
			log.info("Created temporary directory \"{}\".", tempDir);
		}
		else {
			throw new IOException("Cannot create temporary directory \"" + tempDir + "\"");
		}
		try {
			consumer.accept(tempDir.toPath());
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

	public static Path copyResourceToDir(@NonNull String resourceName, @NonNull final Path path) throws IOException {
		if (!resourceName.startsWith("/")) {
			resourceName = '/' + resourceName;
		}
		final Path target = Paths.get(path.toString(), resourceName);
		try (final InputStream in = FileTestUtils.class.getResourceAsStream(resourceName)) {
			Files.copy(in, target);
			log.info("Created file \"{}\".", target);
		}
		return target;
	}

}
