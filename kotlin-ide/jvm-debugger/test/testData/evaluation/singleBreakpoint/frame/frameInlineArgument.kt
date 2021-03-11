package frameInlineArgument

fun main(args: Array<String>) {
    val element = 1
    A().inlineFun {
        //Breakpoint!
        element
    }
}

class A {
    inline fun inlineFun(s: () -> Unit) {
        val element = 1.0
        s()
    }

    val prop = 1
}

// PRINT_FRAME

// EXPRESSION: element
// RESULT: 1: I

