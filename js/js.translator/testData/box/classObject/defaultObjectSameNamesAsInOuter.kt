// EXPECTED_REACHABLE_NODES: 498
package foo

class A {
    private val p: Int
        get() = 4

    companion object B {
        val p: Int
            get() = 6
    }

    fun a() = p + B.p
}


fun box(): String {
    if (A().a() != 10) return "Fail"

    return "OK"
}
