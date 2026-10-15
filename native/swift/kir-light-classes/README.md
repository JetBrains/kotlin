# KIR Light Classes

Lazy implementation of KIR nodes, heavily inspired by Kotlin/JVM light classes and by [`sir-light-classes`](../sir-light-classes/README.md).

Every node wraps the SIR declaration it was translated from, and computes its properties (name, visibility, parent, etc.) only when
they are requested, by calling the providers of [`KirSession`](../kir-providers/src/org/jetbrains/kotlin/kir/providers/KirSession.kt).
