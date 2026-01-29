# Kotlin Gradle Plugin API

Public API surface of the Kotlin Gradle Plugin with binary compatibility guarantees.

## Package Structure

Sources in [`src/common/kotlin/org/jetbrains/kotlin/gradle/`](src/common/kotlin/org/jetbrains/kotlin/gradle):
- [`dsl/`](src/common/kotlin/org/jetbrains/kotlin/gradle/dsl) - Public DSL (compiler options, targets, extensions)
- [`plugin/`](src/common/kotlin/org/jetbrains/kotlin/gradle/plugin) - External integrations API and Gradle attributes
- [`tasks/`](src/common/kotlin/org/jetbrains/kotlin/gradle/tasks) - Task types provided by Kotlin Gradle plugins

Generated sources in [`gen/`](gen) (version constants).

## Binary Compatibility

API stability enforced via [binary-compatibility-validator](https://github.com/Kotlin/binary-compatibility-validator/).

```bash
# Validate public API against snapshot
./gradlew :kotlin-gradle-plugin-api:apiCheck

# Update API snapshot after intentional changes
./gradlew :kotlin-gradle-plugin-api:apiDump
```

API snapshot: [`api/kotlin-gradle-plugin-api.api`](api/kotlin-gradle-plugin-api.api)

## Generated Sources

Version constants generated from [`native-cache-kotlin-versions.txt`](native-cache-kotlin-versions.txt):

```bash
# Regenerate after Kotlin version update
./gradlew :kotlin-gradle-plugin-api:generateKotlinVersionConstant
```

## Related Modules

- [`kotlin-gradle-plugin`](../kotlin-gradle-plugin) - Main plugin implementation
- [`kotlin-gradle-plugin-annotations`](../kotlin-gradle-plugin-annotations) - API annotations
