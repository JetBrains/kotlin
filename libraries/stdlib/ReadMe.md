## The Kotlin Standard Library

This module creates the [standard library for kotlin](http://kotlinlang.org/api/latest/jvm/stdlib/index.html).

### Notes for contributors

We use some code generation to generate the various utility extension function for the various collection-like types like arrays, strings, `Collection<T>`, `Sequence<T>`, `Map<K, V>` etc.

These sources are placed into `generated` folder and their names are prefixed with the underscore, for example `generated/_Collections.kt`

To run the code generator from the `libraries` directory of a kotlin checkout, use the following command:

    ./gradlew :tools:kotlin-stdlib-gen:run

> Note: on Windows type `gradlew` without the leading `./`

This then runs the script which generates a significant part of stdlib sources from the [templates](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-stdlib-gen/src/templates) authored with a special kotlin based DSL.