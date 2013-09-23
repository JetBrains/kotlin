## The Kotlin Standard Library

This module creates the [standard library for kotlin](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/index.html).

### Notes for contributors

We use some code generation to apply the various collection-like methods to various different types like arrays, strings, kotlin.Iterable and java.lang.Iterable etc.

To run the code generator from a kotlin checkout

    cd libraries/tools/kotlin-stdlib-gen
    mvn compile exec:java

This then runs the [GenerateStandardLib.kt](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-stdlib-gen/src/generators/GenerateStandardLib.kt) script to create the source from the files for java.lang.Iterable<T> and java.util.Collection etc.