## The Kotlin Standard Library

This module creates the [standard library for Kotlin](https://kotlinlang.org/api/latest/jvm/stdlib/index.html).

### Code generation

We use code generation to generate some repetitive utility extension functions, e.g. for collection-like types: arrays, strings, `Collection<T>`, `Sequence<T>`, `Map<K, V>` etc.
Those are defined in [templates](../tools/kotlin-stdlib-gen/src/templates) written in a special Kotlin-based DSL.

Generated sources are placed into the `generated` folder and their names are prefixed with an underscore, for example, `generated/_Collections.kt`

To run the code generator, use the following task:

`./gradlew :tools:kotlin-stdlib-gen:run`

### Usage samples

If you want to author samples for the standard library, please head to [the samples readme](samples/ReadMe.md).
