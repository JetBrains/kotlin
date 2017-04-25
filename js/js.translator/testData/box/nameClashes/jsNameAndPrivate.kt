// EXPECTED_REACHABLE_NODES: 493
package foo

@JsName("bar") fun foo(x: Int) = x

private fun bar() = 42

fun box(): String {
    assertEquals(23, foo(23))
    assertEquals(42, bar())

    return "OK"
}