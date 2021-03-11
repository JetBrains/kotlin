// PROBLEM: none
open class A {
    <caret>internal fun foo() {}

    fun bar() {
        O.foo()
    }
}

object O : A()