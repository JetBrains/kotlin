// EXPECTED_REACHABLE_NODES: 501
package foo

var global = ""

open class A {
    open fun foo(x: Int = 23) {
        global += "A.foo($x);"
    }
}

class B : A() {
    override fun foo(x: Int) {
        global += "B.foo($x);"
    }
}

external fun bar(a: A)

fun box(): String {
    bar(A())
    bar(B())

    if (global != "A.foo(23);A.foo(99);B.foo(23);B.foo(99);") return "fail: $global"

    return "OK"
}