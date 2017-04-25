// EXPECTED_REACHABLE_NODES: 496
var log = ""

inline fun f(x: Int): Int {
    val result = x * 2
    log += "f($x)"
    return result
}

fun bar() = 10

class Test {
    val value: Int

    init {
        val x = 3
        val y = f(bar())
        value = x + y
    }
}

fun box(): String {
    val test = Test()
    if (test.value != 23) return "fail: ${test.value}"
    return "OK"
}