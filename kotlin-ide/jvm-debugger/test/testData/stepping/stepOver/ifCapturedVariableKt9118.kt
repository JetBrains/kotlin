package ifCapturedVariableKt9118

fun main(args: Array<String>) {
    //Breakpoint!
    if (1 > 2) {
        println()
    }
    var isCompiledDataFromCache = true
    foo {
        isCompiledDataFromCache = false
    }
}

fun foo(f: () -> Unit) {
    f()
}