package com.github.albertus82.extfix;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public enum Extensions {

	INSTANCE;

	private static final String RESOURCE_NAME = "extensions.lst";

	private final Set<String> set = new TreeSet<>();

	private Extensions() {
		try (final InputStream is = getClass().getResourceAsStream(RESOURCE_NAME)) {
			if (is == null) {
				throw new FileNotFoundException(RESOURCE_NAME);
			}
			try (final InputStreamReader isr = new InputStreamReader(is); final BufferedReader br = new BufferedReader(isr)) {
				String line;
				while ((line = br.readLine()) != null) {
					final StringBuilder sb = new StringBuilder(line.trim().toLowerCase(Locale.ROOT));
					if (sb.length() > 0) {
						if (sb.charAt(0) != '.') {
							sb.insert(0, '.');
						}
						set.add(sb.toString());
					}
				}
			}
		}
		catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static String[] array() {
		return INSTANCE.set.toArray(new String[INSTANCE.set.size()]);
	}

}
