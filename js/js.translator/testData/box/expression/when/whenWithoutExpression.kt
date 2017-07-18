// EXPECTED_REACHABLE_NODES: 990
package foo

fun box() = when {
    1 > 3 -> "fail"
    else -> "OK"
}