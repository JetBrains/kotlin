# Building LLVM for Kotlin/Native

This document describes how to compile LLVM distribution and use it to build Kotlin/Native.  
Usually, you don't need to compile LLVM by yourself: it is downloaded
automatically when you run or build Kotlin/Native compiler.

But if you don't want to download prebuilt LLVM or want to experiment with your own distribution, 
you come to the right place.

## Part 1. Building the right LLVM version.

Use [package.py](tools/llvm_builder/README.md) script to build LLVM distribution the same way the Kotlin team does.

## Part 2. Building Kotlin/Native against given LLVM distribution.

To do so, we need to edit [konan.properties file](konan/konan.properties):
1. `llvmHome.<HOST_NAME>` should point to the freshly built LLVM distribution.
2. `llvmVersion.<HOST_NAME>` should specify its version (for example, `11.1.0`).

Now we are ready to build Kotlin/Native itself. The process is described in [README.md](README.md).

## Q&A

— Can I override `.konan` location?  
— Yes, by setting `$KONAN_DATA_DIR` environment variable. See [HACKING.md](HACKING.md#compiler-environment-variables).

— Can I use another LLVM distribution without rebuilding Kotlin/Native?  
— Yes, see [HACKING.md](HACKING.md#using-different-llvm-distributions-as-part-of-kotlinnative-compilation-pipeline).