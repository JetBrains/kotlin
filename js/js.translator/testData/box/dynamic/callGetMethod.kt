// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1113
package foo

fun box(): String {
    assertEquals("bar.get()", bar.get())
    assertEquals("bar.get(some text)", bar.get("some text"))
    assertEquals("bar.get(1,2,3)", bar.get(1, 2, 3))
    assertEquals("bar.get(1,second,2,3)", bar.get(1, "second", 2, 3))

    return "OK"
}