package suspendImpl

suspend fun main() {
    Foo().foo()
}

open class Foo {
    open fun foo() {
        //Breakpoint!
        val a = 5
        val b = 6
        val c = 7
    }
}

// STEP_OVER: 2