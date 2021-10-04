package com.github.albertus82.extfix;

import java.io.InputStream;
import java.io.PrintStream;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class Console {

	public static final byte DEFAULT_WIDTH = 80;

	@NonNull
	private final InputStream in;

	@NonNull
	private final PrintStream out;

	private final boolean stackTraces;

	private final int width;

	public Console(final boolean stackTraces) {
		this(System.in, System.out, stackTraces, DEFAULT_WIDTH); // NOSONAR Replace this use of System.out or System.err by a logger. Standard outputs should not be used directly to log anything (java:S106)
	}

}
