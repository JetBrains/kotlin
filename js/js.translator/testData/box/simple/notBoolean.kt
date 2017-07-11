// EXPECTED_REACHABLE_NODES: 990
package foo

fun box(): String {
    return if (!false) "OK" else "fail"
}