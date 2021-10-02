File Extension Fix Tool
=======================

## Minimum requirements

* Java SE Development Kit 8

## Build

`./mvnw clean verify`

## Usage

```sh
Usage: extfix [-hLnVX] (-e=<EXT> [-e=<EXT>]... | -f=<FILE>) <BASE_PATH>
File Extension Fix Tool
      <BASE_PATH>     Base directory to scan.
  -e, --ext=<EXT>     File extension to treat.
  -f, --file=<FILE>   File containing a list of extensions to treat.
  -h, --help          Show this help message and exit.
  -L, --links         Follow links.
  -n, --dry-run       Do everything except actually rename the files.
  -V, --version       Print version information and exit.
  -X, --errors        Produce execution error messages.
```
