// EXPECTED_REACHABLE_NODES: 1280
package foo


fun box(): String {
    var a = 3
    a += 3
    return if (a == 6) "OK" else "fail"
}