package hideContinuationThis

suspend fun main() {
    foo()
}

var foo: suspend () -> Unit = {
    //Breakpoint!
    val a = 5
}

// PRINT_FRAME
// SHOW_KOTLIN_VARIABLES