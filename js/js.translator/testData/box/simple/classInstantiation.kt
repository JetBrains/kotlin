// EXPECTED_REACHABLE_NODES: 996
package foo

class Test() {
}

fun box(): String {
    var test = Test()
    return "OK"
}