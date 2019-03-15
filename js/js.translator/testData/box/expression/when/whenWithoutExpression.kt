// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box() = when {
    1 > 3 -> "fail"
    else -> "OK"
}