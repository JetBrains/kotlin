// PROBLEM: none

interface A {
    fun foo(x: Int) = "A$x"
}

open class B {
}

class C : B(), A {
    fun test(): String {
        return <caret>Companion.foo(1)
    }

    companion object {
        fun foo(x: Int) = "C$x"
    }
}
