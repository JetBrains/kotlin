# Kotlin documentation model analyzer

Analyzes Kotlin and Java sources using some Dokka parts and produces a `KdBundle` (aka Kotlin Documentation Model) describing their API and documentation.

## Tests

- Golden tests for complete project models, types, and individual element fields.
- Kotlin source round-trip tests.
- Dokka tests covering the Documentable model for source analysis, KDoc/Javadoc parsing, filtering, links, and multiplatform behavior.

### Golden tests

Golden tests analyze Kotlin and Java fixtures and compare the resulting model, or a focused view of it, with checked-in expected output. Fixtures live under `src/testData`:

| Directory | Generated test class | Expected output |
| --- | --- | --- |
| `projects` | `ProjectsGoldenTestGenerated` | Complete bundle (`.kdm.json`) and declaration signatures grouped by fragment (`.signatures.txt`). |
| `elementFields/types` | `KdmTypesGoldenTestGenerated` | Every outermost type in each element, printed as its path and a text dump of all its fields (`.txt`). |
| `elementFields/<field>` | `AnnotationsGoldenTestGenerated`, `DeprecatedGoldenTestGenerated`, `SinceGoldenTestGenerated`, `ThrowsGoldenTestGenerated`, `OverridensGoldenTestGenerated` | One field (`annotations`, `deprecated`, `since`, `throws`, or `overridens`) from every element (`.txt`). |
| `roundTrip` | `KdmKotlinSourceRoundTripTestGenerated` | The input source itself: canonical declarations are reconstructed from the model and compared with the fixture. |

Fixtures use the shared compiler module extractor for `// MODULE:` and `// FILE:` directives, with a KDM-specific `// PLATFORM:` directive (defaulting to JVM). The third module dependency group, `// MODULE: jvm()()(common)`, maps to source-set `dependsOn` relationships. They can include Kotlin and Java sources, package and module descriptors, and multiplatform source-set dependencies. Round-trip fixtures use canonical declaration-only Kotlin source; bodies, imports, comments, and original formatting are outside their scope.

Expected files are stored beside their fixtures.

### Generating and running tests

All these suites are registered in `src/testFixtures/kotlin/org/jetbrains/kotlin/documentation/analyzer/GenerateDocumentationAnalyzerTests.kt`. After adding fixtures, regenerate the test sources from the repository root:

```bash
./gradlew :tools:kotlin-documentation-model:analyzer:generateTests -q
```

The generated classes go to `build/tests-gen`, not a checked-in directory — do not edit or commit them. The generated tests include checks that every fixture has a test.

Run the generated golden and round-trip suites with the single `test` task (which runs the symbols-based Java analysis):

```bash
./gradlew :tools:kotlin-documentation-model:analyzer:test --tests '*GoldenTestGenerated*' --tests '*SourceRoundTripTestGenerated*' -q
```

To run one suite, replace the filters with its class name from the table.

Golden assertions write the expected file when it is missing and then fail, so to update an expectation, delete it and rerun the test. Review expected-output changes after updating.
