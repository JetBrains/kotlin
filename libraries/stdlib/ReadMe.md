## The Kotlin Standard Library

This module creates the [standard library for Kotlin](https://kotlinlang.org/api/latest/jvm/stdlib/index.html).

### Notes for contributors

We use code generation to generate utility extension functions for some collection-like types like arrays, strings, `Collection<T>`, `Sequence<T>`, `Map<K, V>` etc.

These sources are placed into the `generated` folder and their names are prefixed with an underscore, for example, `generated/_Collections.kt`

To run the code generator, use the following command in the root directory of the project:

    ./gradlew :tools:kotlin-stdlib-gen:run

> Note: on Windows type `gradlew` without the leading `./`

This then runs the script which generates a significant part of stdlib sources from the [templates](../tools/kotlin-stdlib-gen/src/templates) written in a special Kotlin-based DSL.

### Usage samples

If you want to author samples for the standard library, please head to [the samples readme](samples/ReadMe.md).
