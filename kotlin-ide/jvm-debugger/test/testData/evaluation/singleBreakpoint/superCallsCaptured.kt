package superCallsCaptured

fun main() {
    Bar().foo()
}

open class Foo {
    open fun foo() = 5
}

class Bar : Foo() {
    override fun foo(): Int {
        block {
            //Breakpoint!
            val a = this@Bar
        }
        return 6
    }
}

fun block(block: () -> Unit) {
    block()
}

// EXPRESSION: foo()
// RESULT: 6: I

// EXPRESSION: super.foo()
// RESULT: 5: I