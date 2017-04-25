// EXPECTED_REACHABLE_NODES: 493
package foo

class Test() {
    var a: Int = 100
}

fun box(): String {
    var test = Test()
    test.a = 1
    return if (1 == test.a) "OK" else "fail: ${test.a}"
}