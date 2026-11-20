## Description

Contains a plugin for Gradle integration tests 
that helps us to detect deprecation warnings presence regardless of the configured warning mode

Please don't use this module in real projects and don't rely on it as an example. It uses undocumented internal Gradle API.

## Ignoring third-party warnings

Deprecation warnings caused by third-party Gradle plugins, most notably AGP, can neither be fixed nor acted on
from this repository, so the detector skips them. The list of such warnings lives in
[`IgnoredWarnings.kt`](src/common/kotlin/org/jetbrains/kotlin/gradle/test/IgnoredWarnings.kt).

### Adding an entry

1. Learn the exact wording of the warning by running the build with `--warning-mode=all` — Gradle prints the full
   message itself, and `-Dorg.gradle.deprecation.trace=true` adds the stack trace that identifies the caller. Some
   warnings are only emitted once per Gradle daemon, so a warm daemon may stay silent.
2. Add an `IgnoredWarning` whose `reason` links the upstream issue tracking the fix.
3. Keep its patterns narrow enough that the same deprecation caused by Kotlin's own code is still reported, and
   cover it with a test in `IgnoredWarningsTest`.
