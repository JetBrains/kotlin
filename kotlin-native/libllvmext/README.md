# libllvmext

Extensions on top of LLVM for Kotlin/Native.

`src/main/include` contains C headers extending LLVM C API. From these headers Kotlin/JVM bindings
are generated in [llvmInterop](../llvmInterop). Kotlin additions should be prefixed with `LLVMKotlin`.

`src/main/cpp` contains C++ sources. The sources should follow [LLVM coding convention](https://llvm.org/docs/CodingStandards.html).
Kotlin additions should be put inside `llvm::kotlin`.

[FileCheck](https://llvm.org/docs/CommandGuide/FileCheck.html) is used for testing custom LLVM passes.
Use `./gradlew :kotlin-native:libllvmext:test` to run all the tests.
The tests are declared in [testData/fileCheck](testData/fileCheck) as `.ll` files. The tests consist of
- `OPT:` and `FILECHECK:` directives to set additional arguments for `opt` and `FileCheck` tools
- LLVM code on which `opt` will be run
- `CHECK:` directives for `FileCheck`, which are run on the `opt` output;
  see also [`FileCheck` docs](https://llvm.org/docs/CommandGuide/FileCheck.html)) for additional `CHECK-*` directives
- output of `opt` can be found in `build/fileCheck/` with the same name as the test file.

To help with code formatting, [.clang-format](.clang-format) file is placed in this folder.
`:kotlin-native:libllvmext:clangFormat` task can be used to run
`git-clang-format -f $(git merge-base origin/master HEAD) -- kotlin-native/libllvmext/`, which will
format only the changed files. The task accepts optional `--parent=<branch>` (to specify a branch
other than `origin/master`) and `--interactive` (which adds `-p` flag to `git-clang-format` to
interactively accept or reject formatting patches).

## Custom LLVM passes

Custom LLVM passes live in the [Passes](src/main/cpp/Passes) directory:
- `HideSymbolsPass` (`kotlin-hide-symbols`): similar to `internalize` but makes symbols hidden instead
  of internal; the symbols remain visible during compilation, but are hidden by the linker in
  the final binary.
- `PrepareThreadSanitizerPass` (`kotlin-tsan`): function pass, that applies `sanitize_thread` attribute
  to all defined functions; can't simply be done in the code generator, because we want this applied to
  the runtime as well, which is shipped as LLVM bitcode.
- `PrepareStackProtectorPass` (`kotlin-ssp`): function pass, that applies `ssp` attribute to all defined
  functions; can be configured as `kotlin-ssp<strong>` or `kotlin-ssp<req>` to apply `sspstrong` or `sspreq`
  respectively; can't simply be done in the code generator, because we want this applied to the runtime
  as well, which is shipped as LLVM bitcode.
- `RemoveRedundantSafepointsPass` (`kotlin-remove-sp`): function pass, that removes unnecessary prologue
  safepoints from functions; useful, when run after LLVM inlining; can be configured as
  `kotlin-remove-sp<inline>` to additionally inline the remaining safepoints.
- `ModuleCallsCheckerPass` (`kotlin-calls-checker-module`): module pass for external calls checker instrumentation; creates module contructor; should be run after DCE
