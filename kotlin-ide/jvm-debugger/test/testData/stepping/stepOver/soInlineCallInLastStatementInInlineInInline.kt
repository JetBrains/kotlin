package soInlineCallInLastStatementInInlineInInline

fun main(args: Array<String>) {
    bar()
    val i = 45
}

inline fun bar() {
    val a = 1
    foo { 42 }
}

inline fun foo(f: () -> Unit) {
    //Breakpoint!
    f()
}

// STEP_OVER: 4