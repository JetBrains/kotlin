## Kotlin Standard Library for JS

This module produces a `kotlin-stdlib-js` jar which contains all the Kotlin source code for the runtime and standard kotlin library code (both definitions and implementation code) compiled to JavaScript.

The tests of this module can be run and debugged inside any browser by opening the **web/index.html** file in this directory to run the test cases using [Qunit](http://qunitjs.com/).

You should execute `karmaDependencies` gradle task before running these tests to fetch the required Qunit dependency.

These tests are also run during CI build with [gradle karma plugin](https://github.com/craigburke/karma-gradle) in PhantomJS browser.