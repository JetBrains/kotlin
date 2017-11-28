To run tests in node.js your need the following prerequisites installed:

* node.js itself (installation is platform-dependent);
* mocha (`npm install -g mocha`).

Run `nmp install` from this directory to download all additional dependencies for tests.

First, you need to run box tests via JUnit. JUnit tests additionally generate *mocha* runners
(i.e. `.node.js` files).

Second, run `mocha` from this directory.

You can declare `KOTLIN_JS_LOCATION` environment variable to customize location of `kotlin.js` library,
`../../../dist/js/kotlin.js` used by default.