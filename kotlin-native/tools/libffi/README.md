# libffi Build Infrastructure

[package.py](package.py) script automates downloading, patching, building and packing of
libffi distributions used by Kotlin/Native. It follows the same approach as
[llvm_builder](../llvm_builder): the script is the only build entry point, and it is
driven by [manifests.py](manifests.py) declaring,  for every supported target, the libffi version, 
the distribution name and the platform-specific pre-build hooks to run.

### Requirements
* [Python 3](https://www.python.org/)
* a C toolchain for the target (gcc/clang, mingw-w64 for cross-compiling to Windows)
* `make`, `tar`, `patch`, `curl`, `makeinfo` (from `texinfo`, needed to build docs)
* `autoconf`, `automake` and `libtool`, when the manifest requires regenerating the
  `configure` script (e.g. when sources are fetched as a git tag archive, or when a
  hook edits `configure.ac`)

### Pre-build hooks

Hooks are plain shell scripts from the [hooks/](hooks/) directory. `package.py` runs
the hooks listed by the target inside the extracted libffi sources directory,
before configuring, so a hook is free to download patches, edit files or do
anything else the platform requires.

To add support for a new platform quirk:
1. add a self-contained script to `hooks/`;
2. list its path in the `hooks` entry of the relevant target in `manifests.py`.

Note that sources fetched as a git tag archive have no pre-generated `configure`
script: `package.py` regenerates it after running the hooks, and a hook that
edits `configure.ac` (e.g. `docker-friendly-configure.sh`) regenerates the
`configure` script itself.

### Usage

Build libffi for the current host and pack it into the current directory:
```
python3 package.py
```

Cross-compile libffi for Windows x64:
```
python3 package.py --target mingw-x64
```

Build the Intel macOS distribution on an Apple Silicon host (runs under Rosetta):
```
CC="clang -arch x86_64" python3 package.py --target macos-x64
```

Check `python3 package.py -h` for more options, e.g.:
* `--target` selects the target to build for (`--list-targets` shows them all).
* `--output-dir` changes where the archive and its SHA256 are written.
* `--no-pack` keeps the installed distribution directory instead of archiving it.
* `--keep-work-dir` keeps the temporary build directory for inspection.

### Supported targets

The distribution names must match `predefinedLibffiVersions` and the `libffiDir.*`
entries in `konan.properties`.

| Target         | libffi version | Distribution                   | Pre-build hooks                     |
|----------------|----------------|--------------------------------|--------------------------------------|
| `macos-x64`    | 3.2.1          | `libffi-3.2.1-3-darwin-macos`  | `hooks/no-ax-enable-builddir.sh`     |
| `macos-arm64`  | 3.3            | `libffi-3.3-1-macos-arm64`     | `hooks/macos-arm64-support.sh`       |
| `linux-x86-64` | 3.2.1          | `libffi-3.2.1-2-linux-x86-64`  | `hooks/docker-friendly-configure.sh` |
| `linux-arm64`  | 3.2.1          | `libffi-3.2.1-2-linux-arm64`   | `hooks/docker-friendly-configure.sh` |
| `mingw-x64`    | 3.3            | `libffi-3.3-windows-x64-1`    | `hooks/docker-friendly-configure.sh` |

### Docker

Dockerfiles do not clone or patch libffi themselves: they only install the
build dependencies, copy the script, the manifest and the hooks, and run
`package.py`, which pulls libffi from GitHub.

#### Linux

Build libffi for the container architecture (x86-64 or arm64):
```shell
docker build -t kotlin-libffi-builder --file images/linux/Dockerfile .
docker run --rm -it -v <HOST_PATH>:/output kotlin-libffi-builder
```

#### Windows

Cross-compile libffi for Windows x64 using mingw-w64:
```shell
docker build -t kotlin-libffi-builder-mingw --file images/windows/Dockerfile .
docker run --rm -it -v <HOST_PATH>:/output kotlin-libffi-builder-mingw
```
