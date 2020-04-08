package frameLambda

fun main(args: Array<String>) {
    val val1 = 1
    foo {
        //Breakpoint!
        val1
    }
}

fun foo(f: () -> Unit) {
    f()
}

// PRINT_FRAME

// EXPRESSION: val1
// RESULT: 1: I