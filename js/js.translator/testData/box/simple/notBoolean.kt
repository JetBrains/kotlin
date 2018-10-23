// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {
    return if (!false) "OK" else "fail"
}