// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    return if (!false) "OK" else "fail"
}