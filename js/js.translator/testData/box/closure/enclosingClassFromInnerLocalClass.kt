// EXPECTED_REACHABLE_NODES: 519
package foo

open class X(private val x: String) {
    fun foo(): String {
        class B : X("fail1") {
            inner class C {
                fun bar() = x
            }

            fun baz() = C().bar()
        }
        return B().baz()
    }
}

open class Y(private val x: String) {
    fun foo(): String {
        class B {
            inner class C : Y("fail2") {
                fun bar() = x
            }

            fun baz() = C().bar()
        }
        return B().baz()
    }
}

fun box(): String {
    val x = X("OK").foo()
    if (x != "OK") return x

    val y = Y("OK").foo()
    if (y != "OK") return y

    return "OK"
}