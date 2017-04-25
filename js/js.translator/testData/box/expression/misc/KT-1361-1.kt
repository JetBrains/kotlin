// EXPECTED_REACHABLE_NODES: 496
package foo

class B {
    val d = "OK"

    fun f(): String {
        val c = object {
            fun foo(): String {
                return d
            }
            fun boo(): String {
                return foo()
            }
        }
        return c.boo()
    }
}

fun box(): String {
    return B().f()
}