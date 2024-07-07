# Guide to using LitmusKt in Kotlin/Native

This guide is not intended to be complete. Instead, the key points for running and maintaining the LitmusKt integration should be outlined here.

## Basics

* Only `:litmuskt:core` and `:litmuskt:testsuite` parts are currently relevant. Things like JCStress interop and a CLI are currently omitted from this integration.
* The integration is encapsulated into `:litmuskt:repo-tests` subproject.
* To run a test, do the following:
    * Write the test in `:testsuite` as per normal LitmusKt guide
    * Add a wrapper for it into `:repo-tests/testData/standalone` using existing wrappers as reference
    * Run `./gradlew :native:native.tests:generateTests`
    * Then run `./gradlew :litmuskt:repo-tests:nativeTest`
  
## Updating LitmusKt version

In theory, there should have been a git submodule here. In practice, it was decided that no one uses them and it would be simpler to do everything manually.

In `./native/litmuskt`:

1. `git clone https://github.com/JetBrains-Research/litmuskt new-version -b development`
1. Fix the new version files:
    * Only keep `:core` and `:testsuite` subprojects
    * In `:testsuite` build file, add the `:litmuskt` prefix to `:core`, remove all KSP 
    * In `:core` build file, remove `jvmToolchain` references
    * In both build files, remove `java-library` plugin
1. Swap the old `:core` and `:testsuite` subprojects with the new ones
1. Remove `testsuite/src/.../generated/LitmusTestRegistry.kt` and `.../LitmusTestExtensions.kt`
1. Remove `WordTearingNative.kt` test (or find a way to make it compile, with `Bitset` being obsolete and `@ObsoleveNativeApi` being internal)
1. Make sure the wrappers are still correct, in case there were API-breaking changes
1. Keep the `core/nativeMain/src/.../RepoUtils.kt` file in the new version

And that should be it. If something fails, read the generated HTML, it is the only place with detailed error message and stacktrace.