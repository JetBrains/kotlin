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

1. Build PGO instrumented LLVM with ThinLTO.
2. Build Kotlin/Native against it, including the LLVM code linked into both JNI libraries.
3. Compile a training project with that compiler.
4. Merge the training profiles into one shared IR profile.
5. Build optimized LLVM with ThinLTO and that profile, then package both dev and essentials distributions.

Configure the Kotlin/Native build prerequisites first (see
[BUILDING_LLVM.md](../../BUILDING_LLVM.md)). The script locates the Kotlin checkout
from its own path. Supply the training project Git remote, branch or tag, and a shell
command that prepares and compiles the project in a fresh clone:

```shell
python3 kotlin-native/tools/llvm_builder/package-pgo.py \
    --training-repo <git-remote> \
    --training-branch <branch-or-tag> \
    --training-command './gradlew <native-link-task>' \
    --stage0 /absolute/path/to/bootstrap-llvm \
    --llvm-sources /absolute/path/to/llvm-project \
    --xcode-llvm-version 26.4 \
    --install-path /absolute/path/to/llvm-pgo-distribution
```

The workflow switches Xcode between stages: the LLVM builds use `--xcode-llvm-version`
(defaults to 26.4, compatible with LLVM 21), while Xcode with build matching `xcodeBuild`
from `kotlin-native/konan/konan.properties` is used to build Kotlin. Both Xcodes are 
located in `/Applications` by `Contents/version.plist`, and the script
fails before creating the output directory if either is missing. If Kotlin's Xcode is 
configured to be auto provisioned, e.g. via `KONAN_USE_INTERNAL_SERVER`, the latter check 
would be skipped. If the training build project requires yet another Xcode version, set
that via DEVELOPER_DIR env var in `--training-command`. Kotlin and the training project
share one isolated Gradle user home. Before building, the script seeds `caches/modules-2`
and `wrapper/dists` from `GRADLE_USER_HOME` (or `~/.gradle`).

`--target` restricts the build to a single Kotlin/Native target's platform libraries; 
by default platform libraries for all targets are built. `--save-temporary-files` keeps 
the intermediate work directory after a successful run; failed runs always retain it
for diagnostics.

The training compiler build sets `kotlin.internal.native.llvm.pgoInstrument=true`
to link the profile runtime into both JNI libraries. Raw profiles use `%m-%p` to 
separate binaries and processes. Only profiles from the training command are merged, 
using the bootstrap's `llvm-profdata` without `--sparse`.

`--llvm-sources` and `--stage0` are optional. Without them, `package.py` downloads
its default LLVM source and builds the bootstrap. Use matching, fixed LLVM sources
and bootstrap for reproducible builds.

One final LLVM build produces both `llvm-dev/` (full development distribution)
and `llvm/` (essentials), each with a `.tar.gz` archive and `.sha256` checksum,
alongside `common.profdata`. `--distribution-components` controls only essentials
and defaults to:

```shell
--distribution-components clang libclang lld llvm-cov llvm-profdata llvm-ar clang-resource-headers
```

Kotlin/Native is built only for training. Build the final Kotlin/Native
distribution against the optimized LLVM separately.

Following Kotlin Compiler Plugins in Kotlin repo are packaged and used while building
the training project via init script.

- `:plugins:compose-compiler-plugin:compiler:runtimeJar`
- `:kotlinx-serialization-compiler-plugin.embeddable:embeddable`
- `:kotlin-atomicfu-compiler-plugin-embeddable:embeddable`
