# JavaScript Translation

This module performs the translation of Kotlin source code to JavaScript.

There are various Kotlin APIs to JavaScript environments and libraries in the [js.libraries](https://github.com/JetBrains/kotlin/tree/master/js/js.libraries/src) project.

## Compiling the standard Kotlin library to JavaScript

* the [kotlin-js-library](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-js-library) module creates a jar containing all the Kotlin source code for the runtime and standard kotlin library code (both definitions and implementation code) to be compiled to JavaScript.
* the [kotlin-js-tests](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-js-tests) module then compiles a selection of test cases from the [Kotlin standard library for the JVM](https://github.com/JetBrains/kotlin/tree/master/libraries/stdlib) to JavaScript. These tests can then be [run in a web browser using QUnit](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-js-tests/ReadMe.md)
* the [kotlin-js-tests-junit](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-js-tests-junit) then [runs the JavaScript tests for the standard library inside JUnit](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-js-tests-junit/ReadMe.md) using Selenium and the underlying JavaScript & QUnit so that the JavaScript can be tested as part of the maven continuous integration build.

## Reusing JVM based test cases in JavaScript

Any Kotlin test cases using the **org.junit.Test** annotation and the [kotlin.test](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/test/package-summary.html) package, such as [this test case](https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/test/StringTest.kt#L5) are automatically converted to JavaScript using [QUnit](http://qunitjs.com/).

This allows the test cases to be [ran directly in a web page in any web browser](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-js-tests/ReadMe.md).

## Using the Kotlin Library in JavaScript

There is a [simple sample](https://github.com/JetBrains/kotlin/blob/master/libraries/examples/browser-example/ReadMe.md) which shows how to use the [Kotlin Standard Library](http://jetbrains.github.com/kotlin/versions/snapshot/apidocs/index.html) from inside JavaScript in a web page or in a JVM using [kool.io](http://kool.io/)'s [JavaFX browser](https://github.com/koolio/kool/blob/master/samples/kool-template-sample/ReadMe.md)

## Contributing

We love contributions! The JavaScript translation could really use your help! If you fancy contributing:

* check the [contributing section](https://github.com/JetBrains/kotlin/blob/master/ReadMe.md) on general stuff like getting the code etc
* here's the list of [current excluded standard library unit tests](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-js-tests/pom.xml#L46) from the JavaScript JUnit test run; see if you can fix one of those? (Check the TODO commands and the links to specific issues)
* try fix one of the pending [JavaScript translation issues](https://youtrack.jetbrains.com/issues/KT?q=Subsystems:%20%7BBack-end.%20JavaScript%7D%20-Resolved)
