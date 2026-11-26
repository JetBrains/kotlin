# Kotlin LLVM dialect

**NOTE**: this is a work-in-progress.

The ultimate goal of Kotlin LLVM dialect is to be a high-level subset of LLVM.
Kotlin code generator will convert Kotlin IR to this high-level LLVM and later LLVM passes will lower it to the regular LLVM.
The dialect defines which types are available, places restrictions on what function are allowed to do and provides custom intrinsics.

## Dialect

**NOTE**: the dialect is not stable and there are no future stabilization plans.

TBD

## Technical details

Enabled in the compiler with the binary option `useKotlinLLVMDialect=true` (enabled by default).

Binary option `eraseKotlinLLVMDialect=<never|early|late>` can be used for debugging lowering passes.
It erases remnants of the dialect (e.g. attributes and metadata) and can be scheduled right after dialect lowerings (`early`) or after
all LLVM passes (`late`). It makes it possible to check that the generated bitcode is exactly the same between the old approach (code
generator produces low-level LLVM directly) and the dialect approach.

See [README](README.md) for general technical details on writing code in `libllvmext`.

### Intrinsics

TBD

### Passes

The passes can be found in the [Passes](src/main/cpp/Passes) directory:
- `KotlinVerifyPass` (`kotlin-verify`): module pass, that checks LLVM IR conforming to the dialect; should be run before any LLVM lowerings
- `EraseDialectPass` (`kotlin-erase-dialect`): module pass, that erases remnants of Kotlin dialect (e.g. attributes and metadata); for debugging purposes