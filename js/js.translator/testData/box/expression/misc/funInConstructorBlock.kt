// EXPECTED_REACHABLE_NODES: 1380
package foo

class A() {
    fun lold() = "OK"
    val p: () -> String
    init {
        p = { { lold() }() }
    }
}


fun box(): String {
    return A().p()
}
