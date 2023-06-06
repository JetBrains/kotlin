# Dokka's template customization
To provide unified navigation for all parts of [kotlinlang.org](https://kotlinlang.org/),
the Kotlin Website Team uses this directory to place custom templates in this folder
during the website build time on TeamCity.

It is not practical to place these templates in the kotlinx.metadata repository because they change from time to time
and aren't related to the library's release cycle.

The folder is defined as a source for custom templates by the templatesDir property through Dokka's plugin configuration.

[Here](https://kotlinlang.org/docs/dokka-html.html#templates), you can
find more about the customization of Dokka's HTML output.
