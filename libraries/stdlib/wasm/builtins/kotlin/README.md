# Kotlin/Wasm String Implementation Changes

## Overview
This pull request (KT-77056) changes the visibility of two properties in the `String` class from `private` to `internal`:
- `leftIfInSum`: Used for string concatenation operations
- `_chars`: Stores the character array for the string

## Rationale
The visibility change from `private` to `internal` prevents the generation of synthetic accessors when these properties are accessed from other classes within the same module. This optimization improves performance and reduces the size of the generated WebAssembly code.

## Impact
This change is an implementation detail that does not affect the public API of the Kotlin standard library. It only affects the internal implementation of the `String` class for the WebAssembly target.

## Related Issues
- [KT-77056] fix(String): change private declarations to internal