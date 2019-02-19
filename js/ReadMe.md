# JavaScript Translation

This module performs the translation of Kotlin source code to JavaScript.

There are various Kotlin APIs to JavaScript environments in the [standard library](../libraries/stdlib/js).

## Compiling the Kotlin Standard Library for JavaScript

The Kotlin Standard Library for JS is built with gradle, see the corresponding module's [ReadMe](../libraries/stdlib/js/ReadMe.md). 


## Reusing JVM based test cases in JavaScript

Any Kotlin test cases using the **org.junit.Test** annotation and the [kotlin.test](../libraries/kotlin.test) package, such as [this test case](../libraries/stdlib/test/text/StringNumberConversionTest.kt#L16) are automatically converted to JavaScript using [QUnit](https://qunitjs.com/).

This allows the test cases to be [run directly in a web page in any web browser](../libraries/stdlib/js/ReadMe.md).

## Using the Kotlin Library in JavaScript

There is a [simple sample](../libraries/examples/browser-example/ReadMe.md) which shows how to use the [Kotlin Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/index.html) from inside JavaScript in a web page.

## Contributing

We love contributions! The JavaScript translation could really use your help! If you fancy contributing:

* check the [contributing section](https://github.com/JetBrains/kotlin/blob/master/ReadMe.md) on general stuff like getting the code etc
* try fix one of the pending [JavaScript translation issues](https://youtrack.jetbrains.com/issues/KT?q=Subsystems:%20%7BBackend.%20JS%7D%20-Resolved)
