package stepOverInlineFunWithRecursionCall

fun foo(v: Int): Int {
    if (v == 2) {
        //Breakpoint!
        inlineCall { foo(1) }                                           // 1, 2 (lambda)
    }

    return 2 // This line should be visited                             // 3
}


fun main(args: Array<String>) {
    foo(2)                                                              // 4
}

inline fun inlineCall(l: () -> Unit) {
    l()
}

// STEP_OVER: 5