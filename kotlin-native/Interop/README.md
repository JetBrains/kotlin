# Kotlin/Native interop (cinterop)

The `cinterop` tool and the runtime support for C and Objective-C interop.

| Module                          | Purpose                                                                                                          |
|---------------------------------|------------------------------------------------------------------------------------------------------------------|
| `Indexer`                       | Parses C/ObjC headers with libclang (through `:kotlin-native:libclangInterop`) into a native index               |
| `StubGenerator`                 | The `cinterop` entry point (`gen/jvm/main.kt`, class `Interop`). Builds stub IR from the index and emits a klib   |
| `Runtime`                       | The `kotlinx.cinterop` library: `src/main/kotlin` is common, `src/native/kotlin` is part of the Native stdlib, `src/jvm` and `src/callbacks` back the JVM bindings that the compiler itself uses |
| `StubGeneratorConsistencyCheck` | Base test for the JVM interop modules (`llvmInterop`, `libclangInterop`, `common/*`): when prebuilt bindings in `gen/main-<host>` are used, checks that they match freshly generated ones (refresh them with `updatePrebuilt`) |

The pipeline is:
1. A `.def` file and the headers go through `Indexer`.
2. `StubIrDriver` builds stub IR from the index.
3. The Native output is klib **metadata** (`GenerationMode.METADATA`) plus C bridges compiled to bitcode by clang. The compiler
   creates IR for the stubs at link time, in `native/cinterop.deserialization`.
4. The JVM mode emits Kotlin sources and is used for the compiler's own LLVM and libclang bindings.

User documentation: https://kotlinlang.org/docs/native-c-interop.html. Tests: the `cinterop` tag in `native/native.tests`, see
[HACKING.md](../HACKING.md#testing).
