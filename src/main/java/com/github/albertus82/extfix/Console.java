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
	private boolean errors;

	private String currentDirectory = "";

	private boolean firstTime = true;

	public synchronized void printAnalysisProgress(@NonNull final Path path) {
		if (firstTime) {
			out.print(ANALYSIS_PREFIX);
			firstTime = false;
		}
		final String pathString = pathToString(path);
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < currentDirectory.length(); i++) {
			sb.append('\b');
		}
		out.print(sb);
		currentDirectory = StringUtils.abbreviateMiddle(pathString, "...", width - ANALYSIS_PREFIX.length());
		out.print(currentDirectory);
		sb.setLength(0);
		for (int i = ANALYSIS_PREFIX.length() + currentDirectory.length(); i < width; i++) {
			sb.append(' ');
		}
		for (int i = ANALYSIS_PREFIX.length() + currentDirectory.length(); i < width; i++) {
			sb.append('\b');
		}
		out.print(sb);
	}

	public synchronized void printAnalysisMessage(final String message) {
		clearAnalysisLine();
		out.println(message);
		out.print(ANALYSIS_PREFIX);
		out.print(currentDirectory);
	}

	public synchronized void printAnalysisError(final String message, final Throwable e) {
		clearAnalysisLine();
		if (errors && e != null) {
			e.printStackTrace();
		}
		out.println(message);
		out.print(ANALYSIS_PREFIX);
		out.print(currentDirectory);
	}

	public synchronized void clearAnalysisLine() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ANALYSIS_PREFIX.length() + currentDirectory.length(); i++) {
			sb.append("\b \b"); // replace non-whitespace characters with whitespace
		}
		out.print(sb);
	}

	public synchronized void printLine(final String x) {
		out.println(x);
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
