package frameInlineFun

fun main(args: Array<String>) {
    val element = 1
    A().inlineFun {
        element
    }
}

class A {
    inline fun inlineFun(s: (Int) -> Unit) {
        val element = 1.0
        //Breakpoint!
        s(1)
    }

    val prop = 1
}

// PRINT_FRAME

// EXPRESSION: element
// RESULT: 1.0: D

// EXPRESSION: this.prop
// RESULT: 1: I

