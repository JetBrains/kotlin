// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1116
package foo

class Foo(val postfix: String) {
    operator fun invoke(text: String): String {
        return text + postfix
    }
}

fun box(): String {
    val a = Foo(" world!")
    assertEquals("hello world!", a("hello"))
    return "OK"
}
