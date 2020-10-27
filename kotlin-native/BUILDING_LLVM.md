# Building Apple LLVM for Kotlin/Native

This document describes how to compile LLVM distribution and use it to build Kotlin/Native on macOS.  
Usually, you don't need to compile LLVM by yourself:
* If you use Kotlin/Native compiler it will download LLVM on the first run. 
* If you contribute to kotlin-native repository then use `:dependencies:update` Gradle task.

But if you don't want to download prebuilt LLVM or want to experiment with your own distribution, 
you came to the right place.

## Part 1. Building the right LLVM version for macOS.

For macOS host we use LLVM from [Apple downstream](https://github.com/apple/llvm-project). 
Branch is [**apple/stable/20190104**](https://github.com/apple/llvm-project/tree/apple/stable/20190104) 
because it is similar (or even the same) to what Apple ships with Xcode 11.*. 
After cloning the repo and changing the branch we perform the following steps to build LLVM toolchain:

```bash
mkdir build

cd build

cmake -DLLVM_ENABLE_PROJECTS="clang;lld;libcxx;libcxxabi" \
 -DCMAKE_BUILD_TYPE=Release \
 -DLLVM_ENABLE_ASSERTIONS=Off \
 -G Ninja \
 -DCMAKE_INSTALL_PREFIX=clang-llvm-apple-8.0.0-darwin-macos \
 ../llvm

ninja install
```

After these steps `clang-llvm-apple-8.0.0-darwin-macos` directory will contain LLVM distribution that is suitable for building Kotlin/Native.

## Part 2. Building Kotlin/Native against given LLVM distribution.

By default, Kotlin/Native will try to download LLVM distribution from CDN if it is not present in `$HOME/.konan/dependencies` folder. 
There are two ways to bypass this behaviour.

#### Option A. Substitute prebuilt distribution.
This option doesn't require you to edit compiler sources, but a bit harder.

The compiler checks dependency presence by reading contents of `$HOME/.konan/dependencies/.extracted` file. 
So to avoid LLVM downloading, we should manually add a record to the `.extracted` file:
1. Create `$HOME/.konan/dependencies/.extracted` file if it is not created.
2. Add `clang-llvm-apple-8.0.0-darwin-macos` line.

and put `clang-llvm-apple-8.0.0-darwin-macos` directory from the Part 1 to `$HOME/.konan/dependencies/`.

#### Option B. Provide an absolute path to the distribution.
This option requires user to edit [konan.properties file](konan/konan.properties).
Set `llvmHome.<HOST_NAME>` to an absolute path to your LLVM distribution and 
set `llvmVersion.<HOST_NAME>` to its version.
For example, provide a path to `clang-llvm-apple-8.0.0-darwin-macos` from the Part 1 and set version to 8.0.0.

Now we are ready to build Kotlin/Native itself. The process is described in [README.md](README.md).
Please note that we still need to run `./gradlew dependencies:update` to download other dependencies (e.g. libffi).

## Q&A

— Can I override `.konan` location?  
— Yes, by setting `$KONAN_DATA_DIR` environment variable. See [HACKING.md](HACKING.md#compiler-environment-variables).

- Can I use another LLVM distribution without rebuilding Kotlin/Native?
- Yes, see [HACKING.md](HACKING.md#using-different-llvm-distributions-as-part-of-kotlinnative-compilation-pipeline).