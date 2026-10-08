# Commonizer Support Library

This library provides numeric `expect` classes that the commonizer substitutes for inconsistent numeric types.

## Building the Library

```zsh
./gradlew :commonizer-support-library:build :commonizer-support-library:publishToMavenLocal
```

The artifact in `mavenLocal()` must be added to the dependencies of any HMPP module
that uses the numeric `expect` classes coming from commonization to prevent `MISSING_DEPENDENCY_CLASS`:

```kts
allprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.configureEach {
                dependencies {
                    implementation("org.jetbrains.kotlin.commonizer:commonizer-support-library:0.9.9-local")
                }
            }

            compilerOptions.freeCompilerArgs.add("-Xskip-prerelease-check")
        }
    }
}
```

The artifact in `build/` is used by the commonizer itself to load the available support classes.
Historically, this was the main way of accessing them, hence the separation.

## Numeric `expect` Classes Generation

To generate the complete numeric `expect` classes from the templates inside `src/`,
use the following Gradle task:

```zsh
./gradlew :commonizer-support-library:generateSources
```

The generated sources reside in `build/src-gen/`.

But this is anyway done automatically when needed.
