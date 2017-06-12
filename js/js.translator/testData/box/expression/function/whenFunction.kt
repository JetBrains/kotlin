// EXPECTED_REACHABLE_NODES: 488
package foo

fun foo(a: Int): Int = when {
    a == 1 || a + 2 == 3 -> 5
    else -> 6
}
fun box(): String {
    if (foo(1) != 5) return "fail1: ${foo(1)}"
    if (foo(2) != 6) return "fail2: ${foo(1)}"
    return "OK"
}