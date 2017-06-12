// EXPECTED_REACHABLE_NODES: 493
package foo

class Test() {
}

fun box(): String {
    var test = Test()
    return "OK"
}