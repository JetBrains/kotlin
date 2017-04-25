// EXPECTED_REACHABLE_NODES: 487
package foo

fun box() = when {
    1 > 3 -> "fail"
    else -> "OK"
}