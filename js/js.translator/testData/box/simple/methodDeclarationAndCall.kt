// EXPECTED_REACHABLE_NODES: 1284
package foo

class Test() {
    fun method(): String {
        return "OK"
    }
}

fun box(): String {
    var test = Test()
    return test.method()
}