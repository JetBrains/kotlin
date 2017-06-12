// EXPECTED_REACHABLE_NODES: 492
package foo

// CHECK_CONTAINS_NO_CALLS: sum

inline fun <T : Any, R> T.doLet(f: (T) -> R): R = f(this)

private fun sum(x: Int?, y: Int): Int =
        x?.doLet { it + y } ?: 0

fun box(): String {
    assertEquals(5, sum(2, 3))
    assertEquals(0, sum(null, 3))

    return "OK"
}