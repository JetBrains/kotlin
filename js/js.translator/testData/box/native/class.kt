// EXPECTED_REACHABLE_NODES: 488
package foo

external class A(b: Int) {
    fun g(): Int = definedExternally
    fun m(): Int = definedExternally
}


fun box(): String {
    if (A(2).g() != 4) {
        return "fail1"
    }
    if (A(3).m() != 2) {
        return "fail2"
    }
    val a = A(100)
    if (a.g() != 200) {
        return "fail3"
    }
    if (a.m() != 99) {
        return "fail4"
    }
    return "OK"
}
