// EXPECTED_REACHABLE_NODES: 493
package foo

class Test() {
    val a: Int = 100
    var b: Int = a
    val c: Int = a + b
}

fun box(): String {
    val test = Test()
    if (100 != test.a) return "fail1: ${test.a}"
    if (100 != test.b) return "fail2: ${test.b}"
    if (200 != test.c) return "fail3: ${test.c}"

    return "OK"
}
