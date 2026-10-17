# LLVM Build Infrastructure

[package.py](package.py) script automates downloading, building and packing of LLVM distribution used by Kotlin/Native.
### Requirements
* Check [LLVM requirements](https://llvm.org/docs/GettingStarted.html#requirements). Note that:
    * Visual Studio 2017 is required on Windows
    * Xcode is required on macOS
* [Python 3](https://www.python.org/)
* [ninja](https://ninja-build.org/)
* [git](https://git-scm.com/)
* [CMake](https://cmake.org/)

### Usage

Just run
```
python3 package.py
```
It will create a `llvm-distribution` folder which contains LLVM distribution.  
Note that build will take a lot of time and all your machine cores. 😈  
See [HACKING.md](../../HACKING.md#using-different-llvm-distributions-as-part-of-kotlinnative-compilation-pipeline) 
on how to use freshly built distribution.

### Building distribution for end-users
By default, `package.py` installs a lot of LLVM components that is not really required to use Kotlin/Native compiler.
To build only essential parts, run the following command:

```
python3 package.py --build-targets install-distribution --distribution-components $DISTRIBUTION_COMPONENTS
```
Set of required `$DISTRIBUTION_COMPONENTS` depends on OS:

| OS      | Distribution components                                                                |
|---------|----------------------------------------------------------------------------------------|
| Windows | `clang libclang lld llvm-cov llvm-profdata llvm-ar clang-resource-headers`             |
| macOS   | `clang libclang lld llvm-cov llvm-profdata llvm-ar clang-resource-headers`             |
| Linux   | `clang libclang lld llvm-cov llvm-profdata llvm-ar clang-resource-headers compiler-rt` |

### Tuning
Run `python3 package.py -h` to check how one can tune script's behavior.
Some examples:
* `--pack` creates an archive (`zip` on Windows, `tar.gz` on macOS and Linux) with distribution
  alongside with its SHA256.
* `--install-path` allows overriding distribution's output directory.
* `--num-stages` specifies number of steps in build. Passing 2 or more makes bootstrap build which
  means that LLVM will build itself by using distribution from the previous step.
* `--stage0` allows using existing LLVM toolchain for bootstrapping.
* `--lto thin` or `--lto full` enables LTO when building the final LLVM stage.
  Earlier stages keep their original configuration. Omitting this option leaves
  the LTO configuration unchanged.
* `--build-targets` specifies what targets will be passed to Ninja.
* `--distribution-components` is a list of components that will be installed with `install-distribution` build target.

### Docker

#### Linux 

You can use Docker to build LLVM for Linux:
```shell
docker build -t kotlin-llvm-builder --file images/linux/Dockerfile .
docker run --rm -it -v <HOST_PATH>:/output kotlin-llvm-builder --install-path /output/llvm-<version>-x86_64-linux --pack
```

#### Windows 

> [!WARNING]  
> Windows containers support is experimental

If your host machine is Windows, you can use Windows Docker container to build LLVM for Windows: 
```shell 
docker build -t kotlin-llvm-builder --file images/windows/Dockerfile .
docker run --rm -it -v <HOST_PATH>:/output kotlin-llvm-builder --instal-path /output/llvm-<version>-x86_64-windows --pack
```

### Known problems
1. No way to run LLVM tests out of the box.

### IR PGO for the host LLVM toolchain

`package.py --pgo-instrument` builds an IR-instrumented final stage for training.
`--pgo-profile PATH` optimizes the final stage with an indexed profile. These
options are mutually exclusive; earlier stages keep their original configuration.

[package-pgo.py](package-pgo.py) runs the complete workflow on macOS arm64:

1. Build instrumented LLVM with ThinLTO.
2. Build Kotlin/Native against it, including the LLVM code linked into both JNI libraries.
3. Compile a training project with that compiler.
4. Merge the training profiles into one shared IR profile.
5. Build and package optimized LLVM with ThinLTO and that profile.

Configure the Kotlin/Native build prerequisites first (see
[BUILDING_LLVM.md](../../BUILDING_LLVM.md)). The script locates the Kotlin checkout
from its own path. Supply the training Git remote, branch or tag, and a shell
command that prepares and compiles the project in a fresh clone:

```shell
python3 kotlin-native/tools/llvm_builder/package-pgo.py \
    --training-repo <git-remote> \
    --training-branch <branch-or-tag> \
    --training-command './gradlew --no-daemon --no-build-cache --rerun-tasks <native-link-task>' \
    --stage0 /absolute/path/to/bootstrap-llvm \
    --llvm-sources /absolute/path/to/llvm-project \
    --install-path /absolute/path/to/llvm-pgo-distribution
```

`--target` defaults to `ios_simulator_arm64`. Configure the training project's
SDKs and compiler plugins to match the Native compiler. The script sets
`KOTLIN_NATIVE_HOME` and `PATH`; for Gradle, it also sets `kotlin.native.home`,
disables Native toolchain downloading and compiler daemons, and uses an isolated
Gradle home. The training command must use this compiler, execute the Native
compilation tasks, and wait for all profile-writing processes to exit.

The training compiler build sets
`kotlin.internal.native.llvm.pgoInstrument=true` to link the profile runtime into
both JNI libraries. Raw profiles use `%m-%p` to separate binaries and processes.
Only profiles from the training command are merged, using the bootstrap's
`llvm-profdata` without `--sparse`.

`--stage0` and `--llvm-sources` are optional. Without them, `package.py` downloads
its default LLVM source and builds the bootstrap. Use matching, fixed LLVM sources
and bootstrap for reproducible builds.

The final LLVM package defaults to essentials. Like `package.py`, the script
accepts `--build-targets` and `--distribution-components`, each followed by one or
more values. The defaults are:

```shell
--build-targets install-distribution \
--distribution-components clang libclang lld llvm-cov llvm-profdata llvm-ar clang-resource-headers
```

Override the component list as needed, or use `--build-targets install` for a full
development distribution.

The output directory must not exist. It contains `llvm/`, `llvm.tar.gz`,
`llvm.tar.gz.sha256`, and `common.profdata`. The archive and checksum are produced
by `package.py --pack`. Kotlin/Native is built only for training and is not
included in the output package.

`--save-temporary-files` retains intermediate LLVM builds, the training compiler,
the training clone, raw profiles, and isolated Gradle homes. Otherwise
intermediates are removed as stages finish; failed runs retain unfinished
intermediates. The Kotlin checkout's Gradle build outputs remain in place.
