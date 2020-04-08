package inlineLambda

fun main(args: Array<String>) {
    val a = arrayOf(1)
    // EXPRESSION: it
    // RESULT: 1: I
    // RESUME: 1
    //Breakpoint!
    a.map { it * 1 }
}

