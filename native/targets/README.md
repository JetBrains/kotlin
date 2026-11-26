# Kotlin/Native Targets

The Kotlin/Native target model: `KonanTarget` with its `Family` and `Architecture`, and the `Named` interface they implement.
The classes are used by the Kotlin/Native compiler, the Kotlin Gradle plugin (through `kotlin-native-utils`) and the Native platforms
of `:core:language.targets.native`.

The module is published on its own and is exposed as a part of the Kotlin Analysis API. Unless your intent is to add new API for the
Kotlin Analysis API clients, you should avoid putting any new code in this module.

## Binary Compatibility

The module has no dependencies besides the standard library. The compatibility validator plugin and the foreign class usage tracker
ensure that there are no unexpected exposures.
