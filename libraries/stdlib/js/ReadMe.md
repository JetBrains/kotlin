## Kotlin Standard Library for JS

This directory contains Kotlin/JS specific sources of Kotlin standard library
that are used together common sources to produce the `kotlin-stdlib-js` artifact.

Additional sources are copied during the build from `/core/builtins/` except those builtins that 
have a more specific version for K/JS (see `builtins` subdirectory).