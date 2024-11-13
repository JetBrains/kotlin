## Kotlin/JS configuration

This module, despite its name, for historical reasons contains a bunch of different stuff related to the Kotlin/JS compiler.

Right now here we can find:
- The JS AST protobuf serializer. This is no longer used anywhere in the compiler, but it is used by the IntelliJ IDEA metadata decompiler.
  To be removed ([KT-73067](https://youtrack.jetbrains.com/issue/KT-73067)).
- Some incremental compilation helpers (to be moved into some more appropriate place).
