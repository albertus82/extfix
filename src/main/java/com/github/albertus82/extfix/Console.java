package com.github.albertus82.extfix;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Console {

	private static final String ANALYSIS_PREFIX = "Analyzing directory ";

	private static final PrintStream out = System.out; // NOSONAR Replace this use of System.out or System.err by a logger. Standard outputs should not be used directly to log anything (java:S106)

	private final int width;

	@Setter
	private boolean stackTraces;

	private String currentDirectory = "";

	private boolean firstTime = true;

	public Console() {
		this.width = 80;
	}

	public void printAnalysisProgress(@NonNull final Path path) {
		final int limit = width - 1;
		final StringBuilder sb = new StringBuilder();
		if (firstTime) {
			sb.append(ANALYSIS_PREFIX);
			firstTime = false;
		}
		final String pathString = pathToString(path);
		for (int i = 0; i < currentDirectory.length(); i++) {
			sb.append('\b');
		}
		currentDirectory = StringUtils.abbreviateMiddle(pathString, "...", limit - ANALYSIS_PREFIX.length());
		sb.append(currentDirectory);
		for (int i = ANALYSIS_PREFIX.length() + currentDirectory.length(); i < limit; i++) {
			sb.append(' ');
		}
		for (int i = ANALYSIS_PREFIX.length() + currentDirectory.length(); i < limit; i++) {
			sb.append('\b');
		}
		out.print(sb);
	}

	public synchronized void printAnalysisMessage(final String message) {
		clearAnalysisLine();
		out.println(message);
		out.print(ANALYSIS_PREFIX + currentDirectory);
	}

	public synchronized void printAnalysisError(final String message, final Throwable e) {
		clearAnalysisLine();
		if (stackTraces && e != null) {
			e.printStackTrace();
		}
		out.println(message);
		out.print(ANALYSIS_PREFIX + currentDirectory);
	}

	public void clearAnalysisLine() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ANALYSIS_PREFIX.length() + currentDirectory.length(); i++) {
			sb.append("\b \b"); // replace non-whitespace characters with whitespace
		}
		out.print(sb);
	}

	public void printLine(final String x) {
		out.println(x);
	}

	public void print(final String x) {
		out.print(x);
	}

	public void printError(final String message, final Throwable e) {
		if (stackTraces && e != null) {
			e.printStackTrace();
		}
		out.println(message);
	}

	private static String pathToString(@NonNull final Path path) {
		final File file = path.toFile();
		try {
			return file.getCanonicalPath();
		}
		catch (final IOException e) {
			log.debug("Cannot obtain canonical pathname:", e);
			return file.getAbsolutePath();
		}
	}

}
