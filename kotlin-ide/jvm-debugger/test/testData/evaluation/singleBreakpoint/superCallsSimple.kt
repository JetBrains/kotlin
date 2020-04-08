package superCallsSimple

fun main() {
    Bar().foo()
}

open class Foo {
    open fun foo() = 5
}

class Bar : Foo() {
    override fun foo(): Int {
        //Breakpoint!
        return 6
    }
}

// EXPRESSION: foo()
// RESULT: 6: I

// EXPRESSION: super.foo()
// RESULT: 5: I