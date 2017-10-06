// EXPECTED_REACHABLE_NODES: 1108
package foo

fun box(): String {
    return if (!false) "OK" else "fail"
}