// EXPECTED_REACHABLE_NODES: 497
package foo

class Test() {
    var a: Int = 5
        set(b: Int) {
            field = 3
        }
}

fun box(): String {
    var test = Test()
    test.a = 5
    return if (test.a == 3) "OK" else "fail: ${test.a}"
}