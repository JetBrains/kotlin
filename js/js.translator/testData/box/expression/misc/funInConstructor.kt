// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1114
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
