// EXPECTED_REACHABLE_NODES: 499
package foo

fun box(): String {
    assertEquals("bar.invoke(32,object t {})", bar.invoke(32, t))
    assertEquals("bar.invoke(77)", bar invoke 77)
    assertEquals("baz(object t {},object t {})", baz(t, t))

    return "OK"
}