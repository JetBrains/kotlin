# Swift Export

Translates Kotlin declarations to Swift via Analysis API.
Pipeline: Kotlin klib → Analysis API → SirSession → SIR → Swift sources + C header + Kotlin bridges.

→ READ [docs/swift-export/architecture.md](../../docs/swift-export/architecture.md) for the full architecture overview

## Key Concept: SIR

SIR (Swift Intermediate Representation) represents Swift declaration structure only — no function bodies.
It is not a general-purpose Swift AST; it is designed specifically for Kotlin-to-Swift translation.

The core orchestrator is `SirSession` (in `sir-providers/`), decomposed into providers.
This design mirrors `FirSession` and `KaSession`.

## Modules

| Module | Description |
|--------|-------------|
| `sir/` | SIR node types. Auto-generated from DSL — see `sir/tree-generator/` |
| `sir-providers/` | Core translation logic: `SirSession` decomposed into providers (Analysis API → SIR) |
| `sir-light-classes/` | Lazy SIR implementation for IDE use cases (pull-based, on-demand computation) |
| `sir-printer/` | Renders SIR nodes as Swift source text |
| `swift-export-standalone/` | High-level "take files, return files" entry point for Gradle and CLI |
| `swift-export-embeddable/` | Self-contained artifact with embedded (shaded) dependencies |
| `swift-export-ide/` | IDE integration module |
| `swift-export-standalone-integration-tests/` | Integration tests: golden file comparison + Swift execution |

## Testing

### Test groups

Integration tests live in `swift-export-standalone-integration-tests/` with three subgroups: `simple/`, `external/`, `coroutines/`.

Two kinds of tests per group:
- **Generation tests** — compare Swift export output against golden data (`.swift`, `.h`, `.kt` files)
- **Execution tests** — compile generated Swift and run it

### Updating golden files

Use `-Pkotlin.test.update.test.data=true`. The updating run will be RED (failing) — this is expected.

```bash
# Simple tests
./gradlew :native:swift:swift-export-standalone-integration-tests:simple:test \
  --tests "org.jetbrains.kotlin.swiftexport.standalone.test.SwiftExportWithResultValidationTest" \
  -Pkotlin.test.update.test.data=true

# External tests
./gradlew :native:swift:swift-export-standalone-integration-tests:external:test \
  --tests "org.jetbrains.kotlin.swiftexport.standalone.test.ExternalProjectGenerationTests" \
  -Pkotlin.test.update.test.data=true

# Coroutines tests
./gradlew :native:swift:swift-export-standalone-integration-tests:coroutines:test \
  --tests "org.jetbrains.kotlin.swiftexport.standalone.test.SwiftExportCoroutinesWithResultValidationTest" \
  -Pkotlin.test.update.test.data=true
```

**CRITICAL:** After updating golden files, ALWAYS run binary compilation tests.
Validation tests only check that text output matches — they do NOT verify the generated Swift actually compiles.

```bash
# Simple tests
./gradlew :native:swift:swift-export-standalone-integration-tests:simple:test \
  --tests "org.jetbrains.kotlin.swiftexport.standalone.test.SwiftExportWithBinaryCompilationTest"
```

```bash
# Coroutines tests
./gradlew :native:swift:swift-export-standalone-integration-tests:coroutines:test \
  --tests "org.jetbrains.kotlin.swiftexport.standalone.test.SwiftExportWithBinaryCompilationTest" \
```

### Common issue: platform libs NPE

If tests fail with NPE at `createInputModuleForPlatformLibs` or similar platform libs errors, the Kotlin Native distribution needs to be bundled first:

```bash
./gradlew kotlin-native:bundle
```

This takes 30–40 minutes. It only needs to be done once (until the distribution changes).

### Generating test classes

When adding new test data files, regenerate `*Generated.java` test runners:

```bash
./gradlew :generators:sir-tests-generator:generateTests
```

## Pitfall: SIR Type Translation

In `toSirTypeBridge()` (custom type translator):
- Returning `null` means **"no custom bridge needed"** — the caller falls through to regular class resolution in `SirTypeProviderImpl.buildSirType()`
- It does **NOT** mean "unsupported type"
- To mark a type as truly unsupported, the **caller** must produce `SirUnsupportedType`
- `SirUnsupportedType` renders as `Swift.Never` + `fatalError()` stubs (valid Swift)

Do **NOT** call `ctx.reportUnsupportedType()` from inside `toSirTypeBridge()` — it throws immediately and causes `IllegalStateException` during lazy SIR node evaluation (e.g., in `removeConflicts` → `conflictsWith` → `getParameters`).

## Related Documentation

- [Architecture overview](../../docs/swift-export/architecture.md)
- [Kotlin-to-Swift language mapping](../../docs/swift-export/language-mapping.md)
- [Compiler bridges](../../docs/swift-export/compiler-bridges.md)
- [Standalone execution model](../../docs/swift-export/standalone_execution_model.md)
- [USR stability testing](../../docs/swift-export/testing-usr-stability.md)
