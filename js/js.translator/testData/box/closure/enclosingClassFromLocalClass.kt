// EXPECTED_REACHABLE_NODES: 531
package foo

open class A(private val x: String) {
    fun foo(): String {
        class B : A("fail1: simple nested") {
            fun bar() = x
        }
        return B().bar()
    }
}

open class A1(private val x: String) {
    fun foo(): String {
        class B1 {
            fun bar(): String {
                class C1 : A1("fail2: deeply nested") {
                    fun baz() = x
                }
                return C1().baz()
            }
        }
        return B1().bar()
    }
}

open class A2(private val x: String) {
    fun foo(): String {
        class B2 : A2("fail3: deeply nested") {
            fun bar(): String {
                class C2 {
                    fun baz() = x
                }
                return C2().baz()
            }
        }
        return B2().bar()
    }
}

fun box(): String {
    val result = A("OK").foo()
    if (result != "OK") return result

    val result1 = A1("OK").foo()
    if (result1 != "OK") return result1

    val result2 = A2("OK").foo()
    if (result2 != "OK") return result2

    return "OK"
}