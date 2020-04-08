package localFunctionMangling

fun main() {
    fun foo() {}

    //Breakpoint!
    val a = 5
}

// SHOW_KOTLIN_VARIABLES
// PRINT_FRAME

// EXPRESSION: 1
// RESULT: 1: I