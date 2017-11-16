First, you need to run box tests via JUnit. JUnit tests additionally generate *mocha* runners
(i.e. `.node.js` files).

Second, run `./gradlew check` from this directory (`gradlew.bat check` on Windows).

You can declare `KOTLIN_JS_LOCATION` environment variable to customize location of `kotlin.js` library,
`../../../dist/js/kotlin.js` used by default.

In order to use TeamCity reporter run `./gradlew runMochaOnTeamCity`.
