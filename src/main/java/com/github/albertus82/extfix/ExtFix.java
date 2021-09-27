package com.github.albertus82.extfix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command
public class ExtFix implements Callable<Integer> {

	private static final byte[] MAGIC_JPEG = { (byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0 };
	private static final byte[] MAGIC_PNG = { (byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47 };

	@Parameters
	private Path basePath;

	@Option(names = { "-n", "--dry-run" })
	private boolean dryRun;

	public static void main(final String... args) {
		System.exit(new CommandLine(new ExtFix()).setCommandName(ExtFix.class.getSimpleName().toLowerCase(Locale.ROOT)).setOptionsCaseInsensitive(true).execute(args));
	}

	@Override
	public Integer call() throws IOException {
		final Collection<File> files = FileUtils.listFiles(basePath.toFile(), null, true);
		final Map<String, String> renames = new TreeMap<>();
		System.out.println("Found " + files.size() + " files.");
		for (final File f : files) {
			final String contentType = probeContentType(f.toPath());
			if (contentType == null) {
				continue;
			}
			if (contentType.equalsIgnoreCase("jpeg") && !(f.getName().toLowerCase(Locale.ROOT).endsWith(".jpg") || f.getName().toLowerCase(Locale.ROOT).endsWith(".jpeg"))) {
				renames.put(f.getCanonicalPath(), f.getCanonicalPath() + ".jpg");
			}
			if (contentType.equalsIgnoreCase("png") && !f.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
				renames.put(f.getCanonicalPath(), f.getCanonicalPath() + ".png");
			}
		}

		for (final Entry<String, String> e : renames.entrySet()) {
			System.out.println(e.getKey() + " -> " + e.getValue());
			if (!dryRun) {
				Files.move(Paths.get(e.getKey()), Paths.get(e.getValue()));
			}
		}
		System.out.println(renames.size() + " file renamed.");

		return ExitCode.OK;
	}

	private static String probeContentType(final Path path) throws IOException {
		if (path.toFile().length() >= 4) {
			final byte[] header = new byte[4];
			try (final InputStream is = Files.newInputStream(path)) {
				is.read(header);
			}
			if (Arrays.equals(header, MAGIC_JPEG)) {
				return "jpeg";
			}
			else if (Arrays.equals(header, MAGIC_PNG)) {
				return "png";
			}
		}
		return null;
	}

}
