## Kotlin/JS-specific metadata serialization

This module mostly contains the legacy (K1) frontend-specific metadata serialization logic,
as well as some classes that are not really related to that but for historical reasons are stuck here.

When we drop the K1 frontend, we'll probably drop this module entirely and factor out things that don't belong in it.