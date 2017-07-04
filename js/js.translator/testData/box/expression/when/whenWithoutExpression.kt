// EXPECTED_REACHABLE_NODES: 1374
package foo

fun box() = when {
    1 > 3 -> "fail"
    else -> "OK"
}