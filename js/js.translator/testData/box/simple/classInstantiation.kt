// EXPECTED_REACHABLE_NODES: 1282
package foo

class Test() {
}

fun box(): String {
    var test = Test()
    return "OK"
}