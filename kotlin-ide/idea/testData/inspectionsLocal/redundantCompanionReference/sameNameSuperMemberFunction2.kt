// PROBLEM: none

open class A {
    fun foo(x: Int) = "A$x"
}

open class B: A() {
}

class C : B() {
    fun test(): String {
        return <caret>Companion.foo(1)
    }

    companion object {
        fun foo(x: Int) = "C$x"
    }
}
