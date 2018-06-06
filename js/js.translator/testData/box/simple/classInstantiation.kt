// EXPECTED_REACHABLE_NODES: 1114
package foo

class Test() {
}

fun box(): String {
    var test = Test()
    return "OK"
}