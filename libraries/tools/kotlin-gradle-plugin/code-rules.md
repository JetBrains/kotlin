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
