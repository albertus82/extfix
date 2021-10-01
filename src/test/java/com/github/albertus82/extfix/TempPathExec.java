package com.github.albertus82.extfix;

import java.nio.file.Path;

@FunctionalInterface
public interface TempPathExec<T extends Throwable> {

	void execute(Path tempPath) throws T;

}
