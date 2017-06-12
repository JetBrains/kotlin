// EXPECTED_REACHABLE_NODES: 487
package foo


fun box(): String {
    var a = 3
    a += 3
    return if (a == 6) "OK" else "fail"
}