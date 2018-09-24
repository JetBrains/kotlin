// EXPECTED_REACHABLE_NODES: 1280
package foo


fun box(): String {
    for (i in 0.rangeTo(-1)) {
        return "fail"
    }
    return "OK"
}