File Extension Fix Tool
=======================

[![Latest release](https://img.shields.io/github/release/albertus82/extfix.svg)](https://github.com/albertus82/extfix/releases/latest)
[![Build status](https://github.com/albertus82/extfix/actions/workflows/build.yml/badge.svg)](https://github.com/albertus82/extfix/actions)
[![Known Vulnerabilities](https://snyk.io/test/github/albertus82/extfix/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/albertus82/extfix?targetFile=pom.xml)

**Find and rename files with wrong extensions.** File type detection based on [Apache Tika](https://tika.apache.org).

## :warning: Warning

<mark><strong>This utility can severely damage your system. Changing file extensions can cause serious problems, including a complete and irreparable system crash. Do not play with this utility unless you know what you're doing. Never use the `-y` (`--yes`) option unless you know what you're doing. Use this utility at your own risk; I am not responsible for any damage caused to your system.</strong></mark>

## :ballot_box_with_check: Minimum requirements

* Java SE Development Kit 11

## :hammer_and_pick: Build

`./mvnw clean verify`

## :information_source: Usage

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


