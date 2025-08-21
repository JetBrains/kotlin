# Building LLVM for Kotlin/Native

This document describes how to compile LLVM distribution and use it to build Kotlin/Native.  
Usually, you don't need to compile LLVM by yourself: it is downloaded
automatically when you run or build Kotlin/Native compiler.

But if you don't want to download prebuilt LLVM or want to experiment with your own distribution, 
you come to the right place.

## Part 1. Building the right LLVM version.

Use [package.py](tools/llvm_builder/README.md) script to build LLVM distribution the same way the Kotlin team does.

## Part 2. Building Kotlin/Native against given LLVM distribution.

Change `kotlin.native.llvm` Gradle property to the absolute path of the freshly built LLVM distribution.

It's possible to:
* either edit [gradle.properties](gradle.properties),
* or pass `-Pkotlin.native.llvm=<path>` when executing Gradle tasks from the command line.

It's important to correctly specify llvm version. Look up `<version>` in `<llvm-distribution>/lib/clang/<version>/include`.
`kotlin.native.llvm.next.<host>.version` Gradle property is used with llvm distributions provided
by `kotlin.native.llvm=<path>`.

Now we are ready to build Kotlin/Native itself. The process is described in [README.md](README.md).

*NOTE*: In [gradle.properties](gradle.properties) we also have defined special `default` and `next`
LLVM versions. The `default` version is the one Kotlin/Native uses, and the `next` is there to experiment
during migration to another LLVM version.

## Q&A

— Can I override `.konan` location?  
— Yes, by setting `$KONAN_DATA_DIR` environment variable. See [HACKING.md](HACKING.md#compiler-environment-variables).

— Can I use another LLVM distribution without rebuilding Kotlin/Native?  
— Yes, see [HACKING.md](HACKING.md#using-different-llvm-distributions-as-part-of-kotlinnative-compilation-pipeline).