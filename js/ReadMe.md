# JavaScript Translation

This module performs the translation of Kotlin source code to JavaScript.

There are various Kotlin APIs to JavaScript environments in the [standard library](../libraries/stdlib/js).

## Compiling the Kotlin Standard Library for JavaScript

The Kotlin Standard Library for JS is built with gradle, see the corresponding module's [ReadMe](../libraries/stdlib/js-v1/ReadMe.md). 


## Reusing JVM based test cases in JavaScript

Any Kotlin test cases using the **org.junit.Test** annotation and the [kotlin.test](../libraries/kotlin.test) package, such as [this test case](../libraries/stdlib/test/text/StringNumberConversionTest.kt#L16) are automatically converted to JavaScript using [QUnit](https://qunitjs.com/).

This allows the test cases to be [run directly in a web page in any web browser](../libraries/stdlib/js-v1/ReadMe.md).

## Using the Kotlin Library in JavaScript

There is a [simple sample](../libraries/examples/browser-example/ReadMe.md) which shows how to use the [Kotlin Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/index.html) from inside JavaScript in a web page.

## Contributing

We love contributions! The JavaScript translation could really use your help! If you fancy contributing:

* check the [contributing section](https://github.com/JetBrains/kotlin/blob/master/ReadMe.md) on general stuff like getting the code etc
* try fix one of the pending [JavaScript translation issues](https://youtrack.jetbrains.com/issues/KT?q=Subsystems:%20%7BBackend.%20JS%7D%20-Resolved)

## Testing the Kotlin/JS compiler

The following Gradle tasks are responsible for testing the Kotlin/JS compiler:
- `:js:js.tests:jsIrTest` — run JS tests against the IR backend, plus the same tests with dead code elimination (DCE) enabled.
- `:js:js.tests:jsTest` — run JS tests against the legacy backend, plus the same tests with DCE enabled.
- `:js:js.tests:quickTest` — run JS tests against the legacy backend. No DCE.
- `:js:js.tests:test` — run all JS tests, against both the legacy and the IR backends, plus the same tests with DCE enabled.

The JavaScript files generated from the test files are located in the following directories, divided by the translation mode:
- `js/js.tests/build/out` — JS files generated with the `FULL` translation mode (all modules are compiled into one big JS file)
- `js/js.tests/build/out-min` — the same as previous, but with DCE applied.
- `js/js.tests/build/out-per-module` — JS files generated with the `PER_MODULE` translation mode (each module is compiled into a separate JS file)
- `js/js.tests/build/out-per-module-min` — the same as previous, but with DCE applied. 

There are multiple kinds of tests. Here are some of them:

- Box tests. Those are the tests that we are often most interested in, they are located in `js/js.translator/testData/box`.
Such tests must contain a `fn box(): String` function. If this function returns `"OK"`,
the test is considered passed, and if it returns anything else, the test is considered failed.
- Line number tests, located in `js/js.translator/testData/lineNumbers`. These are to ensure that the debug info in the generated JavaScript matches some expectation.
The expectation is written in a comment staring with `// LINES(JS_IR):` or `// LINES(ClassicFrontend JS):` or `// LINES(FIR JS_IR):`.
These comments contain a list of line numbers in the test file that the compiler output is generated from.
The actual line numbers can be viewed in the generated JS file whose name ends with `-lines.js`.
- TypeScript export tests, located in `js/js.translator/testData/typescript-export`.
These test that the generated `.d.ts` (TypeScript definitions) file matches the reference `.d.ts` file.
- Also, some tests located in `compiler/testData/codegen` are shared between the JS and the JVM backends.

### Manually running the generated JS files (IR backend only)
There is a helpful tool for running and debugging JS code generated from test files right in Intellij IDEA.

Note that this will only work for files generated with the IR backend in the `FULL` translation mode,
because these files are self-sufficient and don't require any dependencies.

Just open a generated JS file located in `js/js.tests/build/out`, select
the "run IR test in node.js" run configuration in IDEA, and click on the "Run" or "Debug" icon.
The test output will be printed in the console.

Bonus: debugging is supported! You can set a breakpoint in a test file, open the generated JS file,
select the "run IR test in node.js" run configuration and click on the "Debug" icon.
The execution should pause and the breakpoint will be hit.
This works because a source map located next to the generated JS file.
