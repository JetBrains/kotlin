## Code Generation for Standard Library

Some of the code in the standard library is created by code generation based on templates.
For example, many Array methods need to be implemented separately for Array<T>, ByteArray, ShortArray, IntArray, etc.

To run the code generator from a kotlin checkout

    cd libraries/tools/kotlin-stdlib-gen
    mvn compile exec:java

This then runs the [GenerateStandardLib.kt](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-stdlib-gen/src/generators/GenerateStandardLib.kt) script to create the source from the files for java.lang.Iterable<T> and java.util.Collection etc.
