// PROBLEM: none
// JS

val name: String = ""

fun hello() {
    println("Hello $name!")
}

@JsExport
@JsName("other")
fun <caret>hello(greeting: String) {
    println("$greeting $name!")
}