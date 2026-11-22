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
`--pgo-profile PATH` optimizes the final stage with an indexed profile.

[package-pgo.py](package-pgo.py) runs the workflow on macOS arm64:

1. Build PGO instrumented LLVM with ThinLTO.
2. Build Kotlin/Native against it.
3. Compile a training project with that compiler.
4. Merge the training profiles into one shared IR profile.
5. Build optimized LLVM and package dev and essentials distributions.

```shell
python3 kotlin-native/tools/llvm_builder/package-pgo.py \
    --training-repo <git-remote> \
    --training-branch <branch-or-tag> \
    --training-command './gradlew <native-link-task>' \
    --stage0 /absolute/path/to/bootstrap-llvm \
    --llvm-sources /absolute/path/to/llvm-project \
    --llvm-xcode-version 26.4 \
    --version-number <artifact-revision> \
    --output-dir /absolute/path/to/llvm-pgo-distribution
```

`--output-dir` is the container for the generated distributions and
`common.profdata`. `--version-number` is the positive Kotlin/Native artifact
revision used in both package names. The LLVM build uses
`--llvm-xcode-version` (26.4 by default); the Kotlin/Native Xcode is selected
from `xcodeBuild` in `kotlin-native/konan/konan.properties`.

```text
llvm-21-aarch64-macos-dev-<artifact-revision>/
llvm-21-aarch64-macos-dev-<artifact-revision>.tar.gz
llvm-21-aarch64-macos-essentials-<artifact-revision>/
llvm-21-aarch64-macos-essentials-<artifact-revision>.tar.gz
common.profdata
```

The archive's top-level directory has the same name as its archive stem.
`--distribution-components` controls only essentials; dev is always a full install.
`--kotlin-native-target` restricts Kotlin/Native platform libraries; for example,
use `ios_arm64` for a physical iOS device or `ios_simulator_arm64` for an arm64
simulator. By default all targets are built.
`--save-temporary-files` keeps intermediate files after a successful run, and failed
runs retain their work directory for diagnostics. Kotlin and the training project
share one isolated Gradle home seeded from the global Gradle dependency and wrapper
caches using macOS copy-on-write.

```shell
--distribution-components clang libclang lld llvm-cov llvm-profdata llvm-ar clang-resource-headers
```
