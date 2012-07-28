## The Kotlin Standard Library

This module creates the [standard library for kotlin](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/index.html).

### Notes for contributors

We use some code generation to apply the various collection-like methods to various different types like arrays, strings, kotlin.Iterable and java.lang.Iterable etc.

To run the code generator from a kotlin checkout

    cd libraries/stdlib
    mvn test-compile exec:java

This then runs the [GenerateStandardLib.kt](https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/test/org/jetbrains/kotlin/tools/GenerateStandardLib.kt) script to create the source from the files for java.lang.Iterable<T> and java.util.Collection etc.