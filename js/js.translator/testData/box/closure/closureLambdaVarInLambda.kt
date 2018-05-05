// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1116
// KT-4263 Wrong capturing a function literal variable

package foo

fun box(): String {
    var foo = { 1 }
    var bar = 1

    val t = { "${foo()} $bar" }
    fun b() = "${foo()} $bar"

    foo = { 2 }
    bar = 2

    assertEquals("2 2", t())
    assertEquals("2 2", b())

    return "OK"
}