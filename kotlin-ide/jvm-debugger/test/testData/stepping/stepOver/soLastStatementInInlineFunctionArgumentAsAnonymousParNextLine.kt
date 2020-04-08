package soLastStatementInInlineFunctionArgumentAsAnonymousParNextLine

fun main(args: Array<String>) {
    bar(fun() {
        //Breakpoint!
        nop()
    }
    )
}

inline fun bar(f: () -> Unit) {
    nop()
    f()
}

fun nop() {}

// STEP_OVER: 3