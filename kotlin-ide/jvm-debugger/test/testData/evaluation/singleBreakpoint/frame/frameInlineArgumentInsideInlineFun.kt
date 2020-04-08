// KT-10674: Debugger: Evaluate Expression / Watches fail for variable/parameter captured from one inline function to another
package frameInlineArgumentInsideInlineFun

class A {
    inline fun inlineFun(s: (Int) -> Unit) {
        val element = 1.0
        s(1)
    }
}

class B {
    inline fun foo(s: (Int) -> Unit) {
        val element = 1
        A().inlineFun {
            //Breakpoint!
            val e = element
        }
        s(1)
    }
}

class C {
    fun bar() {
        val element = 1f
        B().foo {
            val e = element
        }
    }
}

fun main(args: Array<String>) {
    C().bar()
}

// PRINT_FRAME

// EXPRESSION: element
// RESULT: 1: I