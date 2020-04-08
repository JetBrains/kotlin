package soLastStatementInInlineFunctionArgumentInGetOperator

fun main(args: Array<String>) {
    12[{
        //Breakpoint!
        nop()
    }]
}

inline operator fun Int.get(f: () -> Unit) {
    nop()
    f()
}                                   // <-- Ideally this line should not be visited

fun nop() {}

// STEP_OVER: 2