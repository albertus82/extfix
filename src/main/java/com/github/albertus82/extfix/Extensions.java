package com.github.albertus82.extfix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.java.Log;
import picocli.CommandLine.Option;

@Log
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE) // for test only access
public class Extensions {

	private final Set<String> set = new TreeSet<>();

	@Option(names = { "-e", "--ext" }, paramLabel = "<EXT>", required = true, description = "File extension to treat.")
	private String[] array;

	@Option(names = { "-f", "--file" }, paramLabel = "<FILE>", required = true, description = "File containing a list of extensions to treat.")
	private Path path;

	public String[] array() {
		if (set.isEmpty()) {
			if (path != null && array == null) {
				try {
					set.addAll(from(path));
				}
				catch (final IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			else if (array != null && path == null) {
				set.addAll(from(array));
			}
			else {
				throw new IllegalStateException();
			}
			log.log(Level.INFO, "Extensions: {0}.", set);
		}
		return set.toArray(new String[set.size()]);
	}

	static Set<String> from(@NonNull final String... array) {
		final Set<String> set = new HashSet<>();
		for (final String item : array) {
			process(item, set);
		}
		return set;
	}

	static Set<String> from(@NonNull final Path path) throws IOException {
		final Set<String> set = new HashSet<>();
		try (final BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			String item;
			while ((item = br.readLine()) != null) {
				process(item, set);
			}
			return set;
		}
	}

	private static void process(@NonNull final String item, @NonNull final Set<String> set) {
		final StringBuilder sb = new StringBuilder(item.trim().toLowerCase(Locale.ROOT));
		if (sb.length() > 0) {
			if (sb.charAt(0) != '.') {
				sb.insert(0, '.');
			}
			set.add(sb.toString());
		}
	}

}
