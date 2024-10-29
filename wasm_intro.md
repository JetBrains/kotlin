
# [kotl.in/wasm-intro](https://kotl.in/wasm-intro)

# Repo: `GitHib/Kotlin`| Branch: `wasm/intro` | `wasm_intro.md`

---

# Kotlin/Wasm Internals 101

## Disclaimers & Notes
- Recording?
- Ask questions on the go.
- Shortcuts.
- As deep as I can, as much as we have time and energy.
- Only about the new frontend (aka K2 aka FIR) + Wasm backend.
- Live demo.

## High level

### Typical user project / Gradle

2 groups of tasks:
- Development
- Production

Tips:
- name of tasks in Gradle could be abbreviated, e.g. use `wasmJsBrProdRun` instead of `wasmJsBrowserProductionRun`

#### Development
Goals:
  - Fast round-trip.
  - Debug experience.

- No optimizations.
- Incremental Compilation:
  - up to 2 times faster now, work to be continued.
  - off by default
  - to enable it add `kotlin.incremental.wasm=true` to `local.properties` or `gradle.properties` https://kotlinlang.org/docs/whatsnew-eap.html#support-for-incremental-compilation
- Going to use custom formatters by default.

#### Production

Goals:
  - Runtime performance.
  - Size.

- Small/local optimizations.
- Remove Unreachable Declarations (aka DCE in the code) 
  - [dce, wasm part](compiler/ir/backend.wasm/src/org/jetbrains/kotlin/backend/wasm/dce)
  - [dce, common & js part](compiler/ir/backend.js/src/org/jetbrains/kotlin/ir/backend/js/dce)
  - Relatively cheap.
  - Use some knowledge about kotlin.
    - Service exports, e.g. for functions.
    - @AssociatedObject (internal feature/API used by kotlinx-serialization)
    - Instantiated classes.
    - Reflection (...::class, KClass<...>)?
  - No de-virtualization.
  - Reachability graph could be dumped.

- Run Binaryen
  - Options: [BinaryenConfig.kt](wasm/wasm.config/src/org/jetbrains/kotlin/platform/wasm/BinaryenConfig.kt)


Simplified Diagram:
```
[Sources]     [Dependencies (.klib files)]
      |         |
       \       / \
        v     v   \
       Kompiler    \
          |         \
          v          |
      [.klib file]   |
          |         /
          v        v
     Kompiler (to produce executable)  + addition lowrings in Prod, including DCE
          |         |
          |         |<Optional>
          v         |
    [.wasm, .mjs]  [.wat]   !! Dev build stops here.
       |
       v
    Binaryen                // Run by Gradle plugin
       |
       v
   [.wasm optimized]        !! In Prod build.
```

### Compiler

- No CLI for K/Wasm, almost
- Compiler options could be found in [K2WasmCompilerArguments.kt](compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/K2WasmCompilerArguments.kt)

#### Pipeline 

Simplified Diagram:
```
    [Sources] & [klibs]
        |
        v
    Frontend (new frontend aka K2 aka FIR)
        |
        v
      [FIR]
        |
        v
      FIR2IR
        |
        v
       [IR] <aka backend IR>
        |
        v
    Compiler plugins
        |
        v
       [IR] <IR>                  !! IR could be serialized to .klib here
        |
..............................................
        |
        v
    Wasm Backend (Lowerings)¹
        |
        v
       [IR lowered/simplified]
        |
        v
       ir2wasm²
        |
        v
     [Wasm IR]--------+
        |             |
        v             |
  WasmIrToBinary³  WasmIrToText⁴
        |             |
        v             v
    [~.wasm]       [~.wat]
```

1: [WasmLoweringPhases.kt:538](compiler/ir/backend.wasm/src/org/jetbrains/kotlin/backend/wasm/WasmLoweringPhases.kt#L538)
2: [ir2wasm](compiler/ir/backend.wasm/src/org/jetbrains/kotlin/backend/wasm/ir2wasm)
3: [WasmIrToBinary.kt](wasm/wasm.ir/src/org/jetbrains/kotlin/wasm/ir/convertors/WasmIrToBinary.kt)
4: [WasmIrToText.kt](wasm/wasm.ir/src/org/jetbrains/kotlin/wasm/ir/convertors/WasmIrToText.kt)

TODO
[Operators.kt](wasm/wasm.ir/src/org/jetbrains/kotlin/wasm/ir/Operators.kt)

## Project

Paths:
- [wasm](wasm)
  - [wasm.config](wasm/wasm.config)
  - [wasm.debug.browsers](wasm/wasm.debug.browsers)
  - [wasm.frontend](wasm/wasm.frontend)
  - [wasm.ir](wasm/wasm.ir)
  - [wasm.tests](wasm/wasm.tests)
- [backend.wasm](compiler/ir/backend.wasm)
- [backend.js](compiler/ir/backend.js)
- [backend.common](compiler/ir/backend.common)
- misc
  - [serialization.js](compiler/ir/serialization.js)
  - [serialization.common](compiler/ir/serialization.common)
  - [ir.tree](compiler/ir/ir.tree)

### Tests

Test running infrastructure is generated from files (or directories) located in special places.

[codegen](compiler/testData/codegen)
- [box](compiler/testData/codegen/box)
- [boxInline](compiler/testData/codegen/boxInline)
- [boxWasmJsInterop](compiler/testData/codegen/boxWasmJsInterop)
- [boxWasmWasi](compiler/testData/codegen/boxWasmWasi)

`fun box() = "OK"`

If you add a new test-data file you need to regenerate tests with gradle task `generateTests` or run configuration in IDE.
`./gradlew generateTests`

Example: [FirWasmJsCodegenBoxTestGenerated.java](wasm/wasm.tests/tests-gen/org/jetbrains/kotlin/wasm/test/FirWasmJsCodegenBoxTestGenerated.java)

#### Run & Debug tests
`./gradlew :wasm:wasm.tests:test`
`./gradlew :wasm:wasm.tests:testFir`

Try to run something. E.g. size tests.
- Navigate or Run with find class/symbol 

Kotlin Test data Helper
https://github.com/demiurg906/test-data-helper-plugin

TODO
Tips:
- IRs
- dump
- custom renderers
- compare files/dirs

#### Directives
compiler/tests-common-new/tests/org/jetbrains/kotlin/test/directives/WasmEnvironmentConfigurationDirectives.kt
most popular ones?

### *.properties
[gradle.properties:123](gradle.properties#L123)

#kotlin.test.junit5.maxParallelForks=1
[gradle.properties:106](gradle.properties#L106)

### Publish
TODO
`./gradlew install`

`defaultSnapshotVersion`
[gradle.properties:27](gradle.properties#L27)
2.1.255-SNAPSHOT
2.<Next>.255-SNAPSHOT

Troubleshooting:
- Stop gradle daemons `./gradlew --stop`
- To change debug level `-i` or `-d`

## Useful Links
- [Docs](https://kotl.in/wasm) 
- [Examples](https://kotl.in/wasm-examples) 
- [Benchmarks](https://kotl.in/wasm-benchmarks)
- More about debugging inside IntelliJ IDEA: 
  - [Debugger Playlist](https://www.youtube.com/playlist?list=PLPZy-hmwOdEUWF85MuwrKV8YVWLmZW4ZA)
  - [Talks by Anton Arhipov](https://www.youtube.com/results?search_query=IntelliJ+IDEA+By+Anton+Arhipov), e.g.:
    - [IntelliJ IDEA Tips & Tricks](https://www.youtube.com/watch?v=53ccfLqYpWY)
    - [Debugging with IntelliJ IDEA](https://www.youtube.com/watch?v=H50J2l2yUAk)


Tips & tricks
- run test from search symbol
- find action
- run anything
- file patterns
