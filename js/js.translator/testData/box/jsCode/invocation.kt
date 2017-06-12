// EXPECTED_REACHABLE_NODES: 493
package foo

fun <A, B, C> run(a: A, b: B, func: (A, B) -> C): C = js("func(a, b)")

fun box(): String {
    assertEquals(3, run(1, 2) { a, b -> a + b})

    return "OK"
}