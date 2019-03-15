// EXPECTED_REACHABLE_NODES: 1281
package foo

var b = 3

fun box(): String {
    b = 2
    return if (b == 2) "OK" else "fail: $b"
}