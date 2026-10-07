# Continuous, error-tolerant incremental compilation for Kotlin/JVM

This feature gives Kotlin/JVM an Eclipse-like edit loop:

1. You save a file.
2. An incremental compilation round runs in-process.
3. The output directory is ready to run again, **even if the code has errors**.

Code with errors is compiled into `throw java.lang.Error("Unresolved compilation problem: ...")`. The rest of the program behaves normally.

```kotlin
fun healthy(): Int = 42                 // works as usual

fun broken(): Int {
    return unresolved()                 // compiles to: throw Error("Unresolved compilation problem:
}                                       //     Unresolved reference 'unresolved'. (Main.kt:4:12)")
```

The compiler prints the errors as warnings, and the compilation finishes with exit code `OK`:

```
warning: Main.kt:4:12: Tolerated error: Unresolved reference 'unresolved'.
```

The feature consists of three parts:

| Part | Location | Purpose |
|------|----------|---------|
| Core hook | `compiler/cli/cli-jvm`, `compiler/fir/fir2ir`, `compiler/frontend.common` | Lets a plugin keep the compiler going past frontend errors and stub out broken bodies |
| Error tolerance compiler plugin | `plugins/error-tolerance/` | Decides *what* to stub and lowers the stubs into `throw`s |
| Incremental compilation (IC) support | `compiler/incremental-compilation-impl`, `build-common`, `core/compiler.common` | Files with tolerated errors are recompiled on the next build |
| Continuous driver | `compiler/build-tools/kotlin-build-tools-continuous` | Watches files and runs IC rounds in-process in a long-lived build session |

The core hook is inactive unless a plugin registers the extension. Without the plugin, compilation behaves exactly as before.

---

## Usage

### 1. One-off compilation from the command line

Build the plugin jar:

```bash
./gradlew :plugins:error-tolerance:compiler-plugin:jar
# -> plugins/error-tolerance/compiler-plugin/build/libs/compiler-plugin-<version>.jar
```

Compile with the plugin:

```bash
kotlinc -Xplugin=plugins/error-tolerance/compiler-plugin/build/libs/compiler-plugin-<version>.jar Main.kt -d out
```

The plugin is enabled by default. To disable it without removing it from the classpath, pass:

```bash
-P plugin:org.jetbrains.kotlin.errortolerance:enabled=false
```

Use the embeddable variant whenever the compiler is `kotlin-compiler-embeddable`, which includes the Build Tools API (BTA) implementation and Gradle:

```bash
./gradlew :plugins:error-tolerance:compiler-plugin-embeddable:jar
```

### 2. Incremental compilation

No extra configuration is needed. When the plugin is enabled in an IC build (Gradle, BTA, or `IncrementalJvmCompilerRunner`):

- A compilation with only tolerated errors **succeeds**. Outputs and caches are updated normally.
- Files with tolerated errors are written to IC's `dirty-sources.txt`. They are **recompiled on the next build even if unchanged**, so an error disappears as soon as the code it depends on is fixed.

### 3. Continuous compilation (watch mode)

`ContinuousJvmCompilation` (module `:compiler:build-tools:kotlin-build-tools-continuous`) runs incremental rounds on file changes. It is built on top of the public Build Tools API, so it works with any BTA implementation of version 2.3 or later.

```kotlin
import org.jetbrains.kotlin.buildtools.api.DelicateBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.continuous.ContinuousJvmCompilation

@OptIn(DelicateBuildToolsApi::class)
fun main() {
    val toolchains = KotlinToolchains.loadImplementation(buildToolsImplClasspath)   // kotlin-build-tools-impl + deps
    toolchains.createBuildSession().use { session ->
        ContinuousJvmCompilation(
            session = session,
            executionPolicy = toolchains.createInProcessExecutionPolicy(),
            sources = initialSources,                                   // List<Path>
            createOperation = { sources, sourcesChanges ->
                toolchains.jvm.jvmCompilationOperationBuilder(sources, destinationDir).apply {
                    compilerArguments.applyCommandLineArguments(
                        listOf(
                            "-classpath", classpath,
                            "-module-name", "app",
                            "-Xplugin=$errorTolerancePluginEmbeddableJar",
                        )
                    )
                    this[JvmCompilationOperation.INCREMENTAL_COMPILATION] =
                        snapshotBasedIcConfigurationBuilder(icWorkingDir, sourcesChanges, dependenciesSnapshotFiles = emptyList())
                            .build()
                }.build()
            },
            listener = { round ->
                println("Round finished: ${round.result} in ${round.durationMillis} ms")
            },
        ).use { compilation ->
            compilation.start()                          // initial round; IC detects what changed since the last build
            compilation.watch(listOf(sourceRoot))        // or: compilation.submitChanges(modified, removed) on save
            readLine()                                   // keep running
        }
    }
}
```

The API surface:

| Member | Description |
|--------|-------------|
| `start()` | Runs an initial round with `SourcesChanges.ToBeCalculated`, so IC finds the changes made while not watching |
| `submitChanges(modified, removed)` | Push API for IDEs and build tools. Unknown files are added to the compilation unit |
| `watch(roots, extensions = {kt, java}, pollIntervalMillis = 200)` | Built-in watcher. It polls modification time and size, which is portable and reliable on macOS, unlike `WatchService` |
| `awaitIdle(timeoutMillis)` | Blocks until all submitted changes are compiled |
| `Listener.onRoundFinished(Round)` | Called after each round with `result`, the compiled `sourcesChanges`, `durationMillis` and `failure` |
| `close()` | Stops the watcher and waits for the current round |

The driver behaves as follows:

- **Batching.** Changes arriving within `debounceMillis` (default 100 ms) are compiled in a single round.
- **No overlap.** Rounds never run concurrently. Changes that arrive during a round are compiled in the next one.
- **Recovery.** If a round throws, the next round uses `SourcesChanges.ToBeCalculated` to resynchronise the IC state.
- **Warm state.** Everything runs inside one `BuildSession`. For in-process execution, the session keeps the compiler application environment alive between rounds.

---

## How it works

```
file save ─► ContinuousJvmCompilation (watcher / push API, batching, one build session)
              └─► BTA JvmCompilationOperation + snapshot-based IC (SourcesChanges.Known)
                    └─► K2 JVM pipeline with the error tolerance plugin
                          frontend  ─► [core]   move error diagnostics out of the collector, report them as warnings
                                    ─► [plugin] compute the plan: which bodies to stub
                          fir2ir    ─► [core]   don't convert stubbed bodies; emit IrErrorExpression(message)
                          IR ext    ─► [plugin] lower error IR into `throw java.lang.Error(message)`
                          codegen   ─► the existing error checks pass: the collector has no errors left
              ◄── result OK + tolerated-error warnings; files with tolerated errors stay dirty for IC
```

### Core hook

**Problem.** By default, the CLI pipeline stops right after the frontend if it reported errors. Every phase has a `CheckCompilationErrors.CheckDiagnosticCollector` post-action, and there are more checks in `convertToIr`, `JvmIrCodegenFactory` and `JvmWriteOutputsPhase`. No plugin extension point can suppress or downgrade errors.

The hook consists of the following pieces:

- **`FirErrorTolerantCompilationExtension`** (`compiler/fir/fir2ir/src/org/jetbrains/kotlin/fir/backend/FirErrorTolerantCompilationExtension.kt`)
  - A new extension point, registered via `CompilerPluginRegistrar.ExtensionStorage`.
  - It has one method, `computeErroneousCodePlan(files, errorsByFile): FirErroneousCodePlan`.
  - `FirErroneousCodePlan.getStubMessage(declaration)` returns a message if the declaration's body must be stubbed.
- **`tolerateFrontendErrorsIfNeeded`** (`compiler/cli/cli-jvm/src/org/jetbrains/kotlin/cli/pipeline/jvm/ErrorTolerantCompilation.kt`)
  - Called at the end of `JvmFrontendPipelinePhase`.
  - If an extension is registered and there are errors, it does the following:
    1. Extracts the errors from the collector (`DiagnosticsCollectorImpl.extractErrors()`).
    2. Reports them as `STRONG_WARNING`s prefixed with `Tolerated error: `.
    3. Notifies the `ToleratedErrorsTracker`.
    4. Stores the plan in the configuration under `ERRONEOUS_CODE_PLAN`.
  - Because no errors are left in the collector, all the downstream checks pass **without being modified**.
- **fir2ir** (`ClassMemberGenerator`, `Fir2IrVisitor`, `Fir2IrConfiguration.erroneousCodePlan`)
  - This generalises kapt's global `skipBodies` mode into a per-declaration check.
  - A stubbed body becomes `return <IrErrorExpression(message)>`, and the broken FIR body is never converted. This avoids fir2ir crashes on malformed FIR.
  - It covers functions, constructors (including the delegation call), property accessors, property initializers and delegates, default argument values, and `init` blocks.
- **Metadata** (`FirJvmSerializerExtension`): error types in signatures are serialised as `error/NonExistentClass`, as kapt already does, instead of crashing.
- **`ToleratedErrorsTracker`** (`core/compiler.common/.../incremental/components/ToleratedErrorsTracker.kt`)
  - A new IC tracker, passed via `Services` and stored under `CommonConfigurationKeys.TOLERATED_ERRORS_TRACKER`.
  - The configuration key is generated by `./gradlew :compiler:config:generateConfigurationKeys`.

### The plugin (`plugins/error-tolerance/compiler-plugin`)

| Module | Contents |
|--------|----------|
| `error-tolerance.k2` | `ErrorTolerantFirExtension`, `ErroneousCodePlanBuilder` (the stubbing policy) |
| `error-tolerance.backend` | `ErrorTolerantIrGenerationExtension` (lowers error IR into `throw`s) |
| `error-tolerance.cli` | `ErrorToleranceCommandLineProcessor`, `ErrorTolerancePluginRegistrar` (plugin id `org.jetbrains.kotlin.errortolerance`) |
| umbrella module | the plugin jar, tests, test data |
| `../compiler-plugin-embeddable` | the shaded jar for `kotlin-compiler-embeddable` |

**The stubbing policy** (`ErroneousCodePlanBuilder`). Each error diagnostic is attributed, by source offset, to the innermost non-synthetic, non-local declaration that contains it. Then:

| Where the error is | What is stubbed |
|--------------------|-----------------|
| Function body (including lambdas and local declarations inside it) | That function's body |
| Default value of a parameter | That default value only |
| Property initializer or delegate | The initializer |
| Custom accessor | That accessor |
| `init` block | That block |
| Function header (e.g. a delegation call `: this(bad)`) | The function's body |
| Property header | The property's initializer and its accessors |
| Class header (supertypes, etc.) | All members of the class |
| Import | Nothing: usages of the unresolved import are reported separately |
| Anywhere else in the file | All declarations in the file |

**Signature propagation.** If a declaration's *resolved signature* contains error types, its JVM signature mentions `error/NonExistentClass`. Examples are an unresolved parameter type, or an implicit return type inferred from a broken body. In that case:

- the declaration is stubbed;
- **every body in the module that references it is stubbed too**, with the message `Usage of 'f' which has compilation errors in its declaration`, followed by the original problems.

**IR lowering** (`ErrorTolerantIrGenerationExtension`). This runs as an `IrGenerationExtension`, before the JVM lowerings:

- `IrErrorExpression` becomes `throw java.lang.Error(message)`.
- `IrErrorCallExpression` evaluates its receiver and arguments, then throws.
- A type operator with an `IrErrorType` operand evaluates the argument, then throws.
- Unresolved supertypes are removed from classes, so a class with a broken header can still be loaded. Its members are all stubbed.

This lowering is also a safety net: any error node that the plan missed becomes a `throw` instead of crashing `ExpressionCodegen`.

### Incremental compilation

`IncrementalCompilerRunner.doCompile` registers a `ToleratedErrorsTrackerImpl` for each round. After a successful round, the files with tolerated errors are written to `dirty-sources.txt`, and recompiled files are removed from it. Without tolerated errors, the file is cleared as before.

---

## Testing

```bash
# Plugin tests (box tests via the real CLI and an IC test with the plugin enabled)
./gradlew :plugins:error-tolerance:compiler-plugin:test

# Continuous driver (end-to-end through the real BTA implementation, in-process)
./gradlew :compiler:build-tools:kotlin-build-tools-continuous:test
```

### Box tests

The test data is in `plugins/error-tolerance/compiler-plugin/testData/box/`. Each `*.kt` file:

1. Is compiled via `K2JVMCompiler` with the plugin jar.
2. Must compile with exit code `OK`.
3. Has its warnings compared with the `*.txt` file next to it. A missing `.txt` file is generated on the first run, and that run fails.
4. Has its `box()` function run, which must return `"OK"`. `box()` checks that healthy code works and broken code throws the expected message.

Current box tests:

| Test | Covers |
|------|--------|
| `bodyErrors.kt` | Unresolved calls, type mismatches, errors inside lambdas |
| `syntaxError.kt` | Syntax errors |
| `members.kt` | Property initializers, `init` blocks, secondary constructor delegation, default values, accessors |
| `headerErrors.kt` | Unresolved parameter and return types, caller propagation, unresolved supertype, erroneous implicit return type |

### IC test

The test is in `testData/incremental/toleratedErrorIsRecompiled/` and uses the standard IC test format (`*.new.N` files and `build.log`). It checks that an unchanged file with a tolerated error is recompiled on the next build, and is no longer recompiled once the error is fixed.

### Continuous driver tests

`ContinuousJvmCompilationTest` covers two scenarios:

- **Submitted changes:** edit → tolerated error (the function throws) → fix in another file (the function works).
- **Watcher:** it picks up a new file and a deleted file. The deleted file's class is removed.

### Regression

The changes to the core were checked against the following suites, with no failures:

- `IncrementalJvmCompilerRunnerTestGenerated` (426 tests)
- kapt (532 tests)
- `FirLightTreeJvmIrTextTestGenerated` (840 tests)

---

## Known limitations

- **Reflection over classes with error-typed signatures.** Plain calls work, but `Class.getMethods()` or `getMethod(...)` on such a class fails with `NoClassDefFoundError: error/NonExistentClass`. Reflection resolves *all* method signatures. The box test harness uses `MethodHandles.findStatic` for this reason.
- **Static initialisation.** An error in a top-level or `const` property initializer throws during class initialisation (`ExceptionInInitializerError`). That makes the whole file facade unusable.
- **Backend errors.** Diagnostics reported by the JVM backend itself (e.g. conflicting JVM signatures) still fail the compilation.
- **Partial coverage.** Errors in type aliases and in enum entry arguments are only covered by the IR safety net (a `throw` at the error site).
- **Out of scope.** KMP (expect/actual), K1, and non-JVM backends aren't supported.
- **Watcher cost.** The watcher walks all source roots every `pollIntervalMillis`. That's fine for a typical module, but not tuned for very large trees. IDEs should use `submitChanges` instead.
- **Warm state.** Only the build session (the compiler application environment) is kept warm between rounds. IC caches are reopened on each round.

## Possible next steps

- Keep `IncrementalJvmCachesManager` open between continuous rounds. This requires separating the lifetime of the caches from the IC transaction in `IncrementalCompilerRunner`.
- Map error types to `Any?` in IR signatures instead of `NonExistentClass`, which would remove the reflection limitation.
- A Gradle subplugin, and wiring the continuous driver into the Kotlin Gradle Plugin (KGP) or a standalone CLI.
- A configurable exception class.
- Handling backend-reported errors.
