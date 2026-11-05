# KIR Providers

The core of Swift import is a [`KirSession`](src/org/jetbrains/kotlin/kir/providers/KirSession.kt) interface which is decomposed into
several components: providers. Each provider contains a small piece of `SIR` -> `KIR` logic which in turn might call other providers.

Such decomposition allows Swift import to be adapted to different use cases: standalone tools, IDE, etc.

This is the Swift import counterpart of [`sir-providers`](../sir-providers/README.md), which is responsible for the `Analysis API` -> `SIR`
translation in Swift export, and it follows the same architecture.
