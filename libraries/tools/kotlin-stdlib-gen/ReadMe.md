## Code Generation for Standard Library

Some of the code in the standard library is created by code generation based on templates.
For example, many `Array` methods need to be implemented separately for `Array<T>`, `ByteArray`, `ShortArray`, `IntArray`, etc.

To run the code generator from the `libraries` directory of a kotlin checkout, use the following command:

    ./gradlew :tools:kotlin-stdlib-gen:run

> Note: on Windows type `gradlew` without the leading `./`

This then runs the script which generates a significant part of stdlib sources from the [templates](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-stdlib-gen/src/templates) authored with a special kotlin based DSL.
