// EXPECTED_REACHABLE_NODES: 1249
package foo

fun box(): String {
    return if (!false) "OK" else "fail"
}