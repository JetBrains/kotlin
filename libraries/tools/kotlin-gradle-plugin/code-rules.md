@test-code-rules.md

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

# Report user-facing problems via KotlinToolingDiagnostics

Applies to:
```
src/
!src/test*/
!src/*Test*/
```

Report problems that users can fix in their build setup (misconfiguration, unsupported or deprecated usage)
as Kotlin tooling diagnostics (declared in `KotlinToolingDiagnostics` and reported with `reportDiagnostic`)
instead of `logger.warn(...)` or throwing `GradleException`, `InvalidUserDataException` or custom exceptions.
This also applies when a change replaces how an existing problem is reported,
e.g. an `error(...)` with a custom exception.
It doesn't apply to internal invariant checks or to failures of external tools and processes during task execution.
It also doesn't apply to code paths users can't reach through their build configuration, or to DSL marked
with `@Deprecated`, which needs no extra report.
