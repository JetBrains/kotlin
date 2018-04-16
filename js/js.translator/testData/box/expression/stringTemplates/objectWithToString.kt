// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1116
package foo

class Foo(val name: String) {
    override public fun toString(): String {
        return name + "S"
    }
}

fun box(): String {
    val a = Foo("abc")
    val b = Foo("def")
    val message = "a = $a, b = $b"
    assertEquals("a = abcS, b = defS", message)
    return "OK"
}
