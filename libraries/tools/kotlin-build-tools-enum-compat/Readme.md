## Description

Backport of `EnumEntries` runtime support API ([KT-48872](https://youtrack.jetbrains.com/issue/KT-48872))

This should help with the case when code was compiled using newer `kotlin-stdlib` version as a `compileOnly` dependency, but at a runtime
older version of `kotlin-stdlib` is provided ([KT-57317](https://youtrack.jetbrains.com/issue/KT-57317)). Mostly it is needed for Kotlin
build tools artifacts.