// EXPECTED_REACHABLE_NODES: 497
package foo

class Test() {
    var a: Int = 1
}

fun box(): String {
    var b = Test()
    assertEquals(1, b.a)
    b.a = 100
    assertEquals(100, b.a)
    return "OK"
}
