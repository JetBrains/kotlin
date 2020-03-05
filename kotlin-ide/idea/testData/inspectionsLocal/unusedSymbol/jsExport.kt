// PROBLEM: none
// JS
class Person(val name: String) {
    fun hello() {
        println("Hello $name!")
    }

    @JsExport
    fun <caret>hello(greeting: String) {
        println("$greeting $name!")
    }
}