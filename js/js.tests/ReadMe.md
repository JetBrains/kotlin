## Testing the Kotlin/JS compiler

This module contains test runner classes and helpers for testing the Kotlin/JS compiler.

The following Gradle tasks make running tests in batch more convenient:
- `:js:js.tests:jsIrTest` — run JS tests with the K1 frontend and with ES5 as the target.
- `:js:js.tests:jsIrES6Test` — run JS tests with the K1 frontend and ES6 as the target.
- `:js:js.tests:jsFirTest` — run JS tests with the K2 frontend and ES5 as the target.
- `:js:js.tests:jsFirES6Test` — run JS tests with the K2 frontend and ES6 as the target.
- `:js:js.tests:test` — run all JS tests

The JavaScript files generated from the test files are located in the following directories, divided by the translation mode:
- `js/js.tests/build/node/out` — JS files generated with the `FULL` translation mode (all modules are compiled into one big JS file)
- `js/js.tests/build/node/out-min` — the same as previous, but with DCE and optional optimizations applied.
- `js/js.tests/build/node/out-per-module` — JS files generated with the `PER_MODULE` translation mode (each module is compiled into a separate JS file)
- `js/js.tests/build/node/out-per-module-min` — the same as previous, but with DCE and optional optimizations applied.

There are multiple kinds of tests. Here are some of them:

- Box tests. Those are the tests that we are often most interested in. 
  They are located in`js/js.translator/testData/box` (Kotlin/JS-specific) and `compiler/testData/codegen/box` (not specific to any backend).
  Such tests must contain a `fun box(): String` function. If this function returns `"OK"`,
  the test is considered passing, and if it returns anything else, the test is considered failing.
- Line number tests, located in `js/js.translator/testData/lineNumbers`.
  These are to ensure that the debug info in the generated JavaScript matches some expectation.
  The expectation is written in a comment staring with `// LINES(JS_IR):` or `// LINES(ClassicFrontend JS):` or `// LINES(FIR JS_IR):`.
  These comments contain a list of line numbers in the test file that the compiler output is generated from.
  The actual line numbers can be viewed in the generated JS file whose name ends with `-lines.js`.
- TypeScript export tests, located in `js/js.translator/testData/typescript-export`.
  These test that the generated `.d.ts` (TypeScript definitions) file matches the reference `.d.ts` file.
- Also, some tests located in `compiler/testData/codegen` are shared among all backends.

### Manually running the generated JS files
There is a helpful tool for running and debugging JS code generated from test files right in IntelliJ IDEA.

Note that this will only work for files generated in the `FULL` translation mode,
because these files are self-sufficient and don't require any dependencies.

Open a generated JS file located in `js/js.tests/build/node/out`, select
the "run IR test in node.js" run configuration in IDEA, and click on the "Run" or "Debug" icon.
The test output will be printed in the console.

Bonus: debugging is supported! You can set a breakpoint in a test file, open the generated JS file,
select the "run IR test in node.js" run configuration and click on the "Debug" icon.
The execution should pause and the breakpoint will be hit.
This works because a source map located next to the generated JS file.
