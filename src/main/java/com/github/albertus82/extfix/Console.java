package com.github.albertus82.extfix;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Console {

	private static final String ANALYZING = "Analyzing directory ";

	private final int width;

	@Setter
	private boolean errors;

	private final StringBuilder currentDirectory = new StringBuilder();

	private boolean firstTime = true;

	@Setter(AccessLevel.PACKAGE)
	private PrintStream out = System.out; // NOSONAR Replace this use of System.out or System.err by a logger. Standard outputs should not be used directly to log anything (java:S106)

	public void printAnalysisProgress(@NonNull final Path path) {
		if (firstTime) {
			out.print(ANALYZING);
			firstTime = false;
		}
		final String pathString = pathToString(path);
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < currentDirectory.length(); i++) {
			sb.append('\b');
		}
		out.print(sb);
		currentDirectory.setLength(0);
		currentDirectory.append(StringUtils.abbreviateMiddle(pathString, "...", width - ANALYZING.length()));
		for (int i = ANALYZING.length() + currentDirectory.length(); i < width; i++) {
			currentDirectory.append(' ');
		}
		out.print(currentDirectory);
	}

	public void printAnalysisMessage(final String message) {
		clearAnalysisLine();
		out.println(message);
		out.print(ANALYZING);
	}

	public void printAnalysisError(final String message, final Throwable e) {
		clearAnalysisLine();
		if (errors && e != null) {
			e.printStackTrace();
		}
		out.println(message);
		out.print(ANALYZING);
	}

	public void clearAnalysisLine() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < currentDirectory.length() + ANALYZING.length(); i++) {
			sb.append('\b');
		}
		out.print(sb);
	}

	public void printLine(final String x) {
		out.println(x);
	}

	private static String pathToString(final Path path) {
		try {
			return path.toFile().getCanonicalPath();
		}
		catch (final IOException e) {
			log.debug("Cannot obtain canonical pathname:", e);
			return path.toFile().getAbsolutePath();
		}
	}

}
