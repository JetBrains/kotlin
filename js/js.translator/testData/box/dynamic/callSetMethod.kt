// EXPECTED_REACHABLE_NODES: 492
package foo

fun box(): String {
    assertEquals("bar.set()", bar.set())
    assertEquals("bar.set(some text)", bar.set("some text"))
    assertEquals("bar.set(1,2,3)", bar.set(1, 2, 3))
    assertEquals("bar.set(1,second,2,3)", bar.set(1, "second", 2, 3))

    return "OK"
}