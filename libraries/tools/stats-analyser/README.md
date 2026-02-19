# Kotlin Stats Analyser

The light-weight console tool that allows analysis JSON performance reports obtained by running `kotlinc` with passed `-Xdump-perf=<path-to-reports-directory>/*.json` or `Xdump-perf=<path-to-reports-directory>`.

Currently, it doesn't depend on the superior build system.
The only thing is needed to collect reports: pass `-Xdump-perf` argument to kotlin compiler somehow.

The tool scans all JSON files, calculates aggregated info, and prints different metrics into the console in Markdown format.
It has two modes: `module` and `timestamp`.
In both modes the fool prints aggregated info for all found modules: total time, analysis time, lines per second, and so on.
Other metrics can be easily added if necessary.

## Module mode

In this mode the tool collects all info on a per-module basis. If it encounters a report with an already existing module name,
the oldest report is filtered out.

### Example output

```
# Stats for 2025-06-16T15:21:09

...

# Slowest modules

## By total time

| Module   |            Value |
| -------- | ---------------: |
| module-1 | 41.12% (5371 ms) |
| module-0 | 30.06% (3927 ms) |
| module-2 | 28.82% (3764 ms) |
```

It prints the latest timestamps over all reports in the title.

## Timestamp mode

In this mode the tool collects all reports for the provided module name.
It's useful when you want to test performance on a single module but with different compiler or user-code optimizations.
For instance, when you are trying to figure out, if replacing star imports with explicit ones affects performance.
To run the tool in timestamp mode, put the module name that you want to analyze as a second argument:

```
stats-analyser <path-to-directory-with-json-reports> <your-module-name>
```

### Example output

```
Stats for my-module

...

# Slowest runs

## By total time

| Time Stamp          |            Value |
| ------------------- | ---------------: |
| 2025-06-16T15:21:09 | 41.12% (5371 ms) |
| 2025-06-13T22:26:25 | 30.06% (3927 ms) |
| 2025-06-13T22:27:09 | 28.82% (3764 ms) |
```

## Prospects

If necessary, the tool can be extended by implementing the following features:

* Different output formats (HTML, JSON)
* Sending reports to a remote server (integrating with FUS)
* Support for more extended build system reports (JPS, Gradle) that include, for example, info about Java compilation
* One tool to analyze them all: integrate the functionality of **modularized-tests** module into with this tool
