File Extension Fix Tool
=======================

[![Build Status](https://github.com/albertus82/extfix/workflows/build/badge.svg)](https://github.com/albertus82/extfix/actions)
[![Build status](https://ci.appveyor.com/api/projects/status/github/albertus82/extfix?branch=master&svg=true)](https://ci.appveyor.com/project/albertus82/extfix)
[![Known Vulnerabilities](https://snyk.io/test/github/albertus82/extfix/badge.svg?targetFile=pom.xml)](https://snyk.io/test/github/albertus82/extfix?targetFile=pom.xml)

## Minimum requirements

* Java SE Development Kit 8

## Build

`./mvnw clean verify`

## Usage

```sh
Usage: extfix [-hLnVXy] [-e=<EXT> [-e=<EXT>]... | -f=<FILE>] <BASE_PATH>
File Extension Fix Tool
      <BASE_PATH>     Base directory to scan.
  -e, --ext=<EXT>     File extension to treat.
  -f, --file=<FILE>   File containing a list of extensions to treat.
  -h, --help          Show this help message and exit.
  -L, --links         Follow links.
  -n, --dry-run       Do everything except actually rename the files.
  -V, --version       Print version information and exit.
  -X, --errors        Produce execution error messages.
  -y, --yes           Automatic yes to prompts (run non-interactively).
```
