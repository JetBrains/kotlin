// EXPECTED_REACHABLE_NODES: 493
package foo

class A() {
    fun lold() = "OK"

    val p = {
        {
            lold()
        }()
    }
}

fun box(): String {
    return A().p()
}
