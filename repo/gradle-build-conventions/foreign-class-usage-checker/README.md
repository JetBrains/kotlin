# Foreign Class Usage Checker

A Gradle plugin that tracks and validates the usage of external classes (from dependencies and other modules) in the public API surface of a
project.

## Motivation

This plugin was created to support the separation of the Kotlin Analysis API and PSI API into distinct API and implementation layers. By
tracking which external classes appear in the public API surface, it helps ensure that compiler internals remain hidden from the classpath
exposed to users.

The plugin serves as a safeguard against unintended API exposure: when the set of foreign classes changes, the build fails, prompting
developers to review whether the change is intentional and acceptable.

The plugin respects visibility modifiers and can exclude API elements marked with specific annotations (e.g., `@KaImplementationDetail`,
`@KtImplementationDetail`).

## Usage

### 1. Apply the Plugin

Add the plugin to your `build.gradle.kts`:

```kotlin
plugins {
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}
```

### 2. Register the Tasks

Declare a dump and its configuration once; the helper registers a pair of tasks that share it:

```kotlin
import org.jetbrains.kotlin.build.foreign.registerForeignClassUsageTasks

registerForeignClassUsageTasks {
    outputFile = file("api/my-module-api.foreign")
    nonPublicMarkers.addAll(
        listOf(
            "com.example.InternalApi",
            "com.example.ExperimentalApi"
        )
    )
}
```

`checkForeignClassUsage` reports a difference without touching the dump, and `updateForeignClassUsage` rewrites it. Pass `nameSuffix` to
declare more than one dump in a module, as `:analysis:analysis-api` does for its unstable API.

### 3. Run the Tasks

The verifying task is added to the `check` and `checkApiSurface` lifecycle tasks, so it runs during:

```bash
./gradlew check
```

The rewriting task is added to `updateApiSurface`. You can also run either separately:

```bash
./gradlew checkForeignClassUsage
./gradlew updateForeignClassUsage
```

Do not ask for both in one build: verifying a dump that the same build rewrites is contradictory, and Gradle fails on it.

## Configuration Options

`ForeignClassUsageTask` supports the following configuration properties:

### `outputFile` (required)

Type: `RegularFileProperty`

The file holding the foreign class names. `updateForeignClassUsage` creates it if it is missing, rewrites it if the content differs, and
fails the build either way, so that the change gets reviewed. `checkForeignClassUsage` only reports what differs.

### `nonPublicMarkers`

Type: `SetProperty<String>`
Default: empty set

Fully qualified names of annotations that mark API elements as non-public. Elements annotated with these markers are excluded from analysis.

Example:

```kotlin
nonPublicMarkers.addAll(
    listOf(
        "org.jetbrains.kotlin.psi.KtImplementationDetail",
        "org.jetbrains.kotlin.analysis.api.KaImplementationDetail"
    )
)
```

### `ignoredPackages`

Type: `SetProperty<String>`
Default: `["java", "kotlin", "org.jetbrains.annotations"]`

Packages whose classes should be excluded from the output. Classes from these packages are not considered "foreign" for tracking purposes.

### `collectUsages`

Type: `Property<Boolean>`
Default: `false`

When enabled, the output file includes not only foreign class names but also lists which classes in your module reference each foreign
class. `collectUsages` is useful for tracking down unexpected usages of foreign classes in your module's public API surface.

## Output Format

The output file contains one class name per line in JVM internal format:

```
com/google/common/collect/ImmutableBiMap
com/google/common/collect/ImmutableSet
com/intellij/psi/PsiElement
com/intellij/psi/PsiFile
org/jetbrains/kotlin/name/Name
org/jetbrains/kotlin/psi/KtElement
org/jetbrains/kotlin/psi/KtFile
```

Nested classes are excluded if their outer class is already in the list (e.g., if `com/foo/Bar` is present, `com/foo/Bar$Inner` is omitted).

When `collectUsages` is enabled, the format includes the referencing classes:

```
com/intellij/psi/PsiElement
    com/example/MyClass
    com/example/AnotherClass

com/intellij/psi/PsiFile
    com/example/MyClass
```

## Workflow

1. Make changes to your module's public API
2. Run `./gradlew check` (or specifically `./gradlew checkForeignClassUsage`)
3. If the foreign class usage changed:
    - The build fails, listing what the dump no longer expects and what is newly used
    - Run `./gradlew updateForeignClassUsage` to rewrite the `.foreign` file
    - Review the changes in the `.foreign` file
    - If the changes are acceptable, commit the updated file
    - Re-run the build (it will now pass)

This workflow ensures that changes to the API surface are explicit and intentional.
