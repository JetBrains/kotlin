// EXPECTED_REACHABLE_NODES: 990
package foo


fun box(): String {
    for (i in 0.rangeTo(-1)) {
        return "fail"
    }
    return "OK"
}