// EXPECTED_REACHABLE_NODES: 996
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
