# Avoid path resolve outside the parent directory

Applies to:
```
**/src/**/*.kt
```

File.resolve(relative) or Path.resolve(relative) can be dangerous when argument `relative` comes from 
external world not controlled by the user. I.e. dependency resolution or other tool's output.
As it can lead to arbitrary file access. via path traversal techniques.

Kotlin Gradle Plugin has utility function `resolveInside` that should be used instead to ensure safety.

# Plugin conventions are followed

Applies to:
```
src/**/*.kt
```

Changes should follow conventions defined in `@docs/build-tools/gradle/conventions/*.md`.

# Kotlin Build Tools libraries stay off the buildscript classpath when they have dependencies

Applies to:
```
build.gradle.kts
```

KGP may depend on a `:build-tools:libraries:*` project with `commonImplementation`, `commonApi` or
`embedded` only if its runtime dependencies are stdlib, the BTA API and other libraries that meet the same
condition. A library that brings kotlinx-coroutines, kotlinx-serialization, Ktor or any other third-party
artifact must be split into an API and an implementation module, and KGP must load the implementation in an
isolated worker from a resolvable configuration instead. See "Loading an implementation in KGP" in
`build-tools/libraries/AGENTS.md`.
