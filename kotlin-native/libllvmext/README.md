# libllvmext

Extensions on top of LLVM for Kotlin/Native.

`src/main/include` contains C headers extending LLVM C API. From these headers Kotlin/JVM bindings
are generated in [llvmInterop](../llvmInterop). Kotlin additions should be prefixed with `LLVMKotlin`.

`src/main/cpp` contains C++ sources. The sources should follow [LLVM coding convention](https://llvm.org/docs/CodingStandards.html).
Kotlin additions should be put inside `llvm::kotlin`.

To help with code formatting, [.clang-format](.clang-format) file is placed in this folder.
`:kotlin-native:libllvmext:clangFormat` task can be used to run
`git-clang-format -f $(git merge-base origin/master HEAD) -- kotlin-native/libllvmext/`, which will
format only the changed files. The task accepts optional `--parent=<branch>` (to specify a branch
other than `origin/master`) and `--interactive` (which adds `-p` flag to `git-clang-format` to
interactively accept or reject formatting patches).
