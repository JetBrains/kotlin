// EXPECTED_REACHABLE_NODES: 491
class SimpleClass() {
    fun foo() = 610
}

fun box(): String {
    val c = SimpleClass()
    if (c.foo() == 610) {
        return "OK"
    }
    return "FAIL"
}
