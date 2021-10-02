package com.github.albertus82.extfix;

import java.nio.file.Path;

@FunctionalInterface
public interface TempDirExec<T extends Throwable> {

	void execute(Path tempDir) throws T;

}
