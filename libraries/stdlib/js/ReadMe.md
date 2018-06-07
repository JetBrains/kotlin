## Kotlin Standard Library for JS

This module produces a `kotlin-stdlib-js` jar which contains all the Kotlin standard kotlin library code compiled to JavaScript.

The tests of this module can be run and debugged inside any browser by opening the **web/index.html** file in this directory to run the test cases using [Mocha](https://mochajs.org/).

You should execute `installMocha` gradle task before running these tests to fetch the required Mocha dependency and `testClasses` task to compile test code.

These tests are also run during CI build with [Node.js plugin](https://github.com/srs/gradle-node-plugin).