## Sample Application

This (really simple ;) application shows how to use Kotlin and the maven plugin to generate JavaScript and invoke it from inside a HTML web page.

The source [Hello.kt](https://github.com/JetBrains/kotlin/blob/master/libraries/examples/browser-example-with-library/src/main/kotlin/sample/Hello.kt) uses the [kotlin.browser](https://jetbrains.github.com/kotlin/versions/snapshot/apidocs/kotlin/browser/package-summary.html) API to access the *document* property to modify the HTML.

### Running the sample in a web browser

To run the example try:

    cd libraries/examples/browser-example-with-library
    mvn install
    open sample.html

This should open a browser which then shows some simple HTML which then includes some dynamically generated content.
