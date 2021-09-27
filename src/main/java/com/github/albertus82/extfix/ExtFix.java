package com.github.albertus82.extfix;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.io.filefilter.CanReadFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeTypeException;

import lombok.extern.java.Log;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Log
@Command
public class ExtFix implements Callable<Integer> {

	private static final String[] SUFFIXES = { ".png", ".jpg", ".jpeg" };

	final TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
	final Tika tika = new Tika(tikaConfig);

	@Parameters
	private Path basePath;

	@Option(names = { "-n", "--dry-run" })
	private boolean dryRun;

	public static void main(final String... args) {
		System.exit(new CommandLine(new ExtFix()).setCommandName(ExtFix.class.getSimpleName().toLowerCase(Locale.ROOT)).setOptionsCaseInsensitive(true).execute(args));
	}

	private int count = 0;

	@Override
	public Integer call() throws IOException {
		basePath = basePath.toFile().getCanonicalFile().toPath();
		final Map<String, String> renames = new TreeMap<>();
		final Stream<Path> stream = PathUtils.walk(basePath, CanReadFileFilter.CAN_READ.and(new SuffixFileFilter(SUFFIXES, IOCase.INSENSITIVE)), Short.MAX_VALUE, false, FileVisitOption.FOLLOW_LINKS);
		stream.filter(path -> path.getFileName() != null).forEach(path -> {
			count++;
			final File file = path.toFile();
			try {
				final String extension = FilenameUtils.getExtension(file.getName());
				final String mediaType = tika.detect(path);
				if (mediaType == null) {
					log.log(Level.WARNING, "Cannot determine type of ''{0}''.", path);
				}
				else {
					final List<String> extensions = tikaConfig.getMimeRepository().forName(mediaType).getExtensions();
					log.log(Level.FINE, "{0} - {1}", new Object[] { extensions, path });
					if (!extensions.isEmpty() && extensions.stream().noneMatch(e -> e.equalsIgnoreCase('.' + extension))) {
						final String oldName = file.getCanonicalPath();
						final String newName = file.getCanonicalPath() + extensions.get(0);
						renames.put(oldName, newName);
						log.log(Level.FINE, "{0} -> {1}", new String[] { oldName, newName });
					}
				}
			}
			catch (final MimeTypeException | RuntimeException | IOException e) {
				log.log(Level.WARNING, e, () -> "Skipped '" + path + "':");
			}
		});

		log.log(Level.INFO, "{0} files analyzed.", count);

		for (final Entry<String, String> e : renames.entrySet()) {
			log.log(Level.INFO, "{0} -> {1}", new String[] { e.getKey(), e.getValue() });
			if (!dryRun) {
				Files.move(Paths.get(e.getKey()), Paths.get(e.getValue()));
			}
		}
		log.log(Level.INFO, "{0} files renamed.", renames.size());

		return ExitCode.OK;
	}

}
