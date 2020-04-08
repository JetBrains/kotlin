package soLastStatementInInlineFunctionArgumentInNonInlineCall

fun main(args: Array<String>) {
    nonInline { bar {
        //Breakpoint!
        nop()
    } }
}

fun nonInline(f: () -> Unit) {
    f()
}

inline fun bar(f: () -> Unit) {
    nop()
    f()
}                                  // <-- Ideally this line should not be visited

fun nop() {}


// STEP_OVER: 2