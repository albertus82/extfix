File Extension Fix Tool
=======================

[![Build Status](https://github.com/albertus82/extfix/workflows/build/badge.svg)](https://github.com/albertus82/extfix/actions)
[![Build status](https://ci.appveyor.com/api/projects/status/github/albertus82/extfix?branch=main&svg=true)](https://ci.appveyor.com/project/albertus82/extfix)
[![Known Vulnerabilities](https://snyk.io/test/github/albertus82/extfix/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/albertus82/extfix?targetFile=pom.xml)

**Find and rename files with wrong extensions.** File type detection based on [Apache Tika](https://tika.apache.org).

## Minimum requirements

* Java SE Development Kit 8

## Build

`./mvnw clean verify`

## Usage

```sh
Usage: extfix [-hLnRVXy] [-e=<EXT>[,<EXT>...]]... <PATH>
File Extension Fix Tool
      <PATH>        Directory to scan for files with invalid extension.
  -e, --extensions=<EXT>[,<EXT>...]
                    File extensions to filter.
  -h, --help        Show this help message and exit.
  -L, --links       Follow links.
  -n, --dry-run     Do everything except actually rename the files.
  -R, --recursive   Operate on files and directories recursively.
  -V, --version     Print version information and exit.
  -X, --errors      Produce execution error messages.
  -y, --yes         Automatic yes to prompts (run non-interactively).
```
