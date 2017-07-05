// EXPECTED_REACHABLE_NODES: 1374
package foo

fun box(): String {

    while (2 < 1) {
        return "fail"
    }
    return "OK"
}

