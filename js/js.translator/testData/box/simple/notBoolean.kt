// EXPECTED_REACHABLE_NODES: 1374
package foo

fun box(): String {
    return if (!false) "OK" else "fail"
}