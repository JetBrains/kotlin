// EXPECTED_REACHABLE_NODES: 1255
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
