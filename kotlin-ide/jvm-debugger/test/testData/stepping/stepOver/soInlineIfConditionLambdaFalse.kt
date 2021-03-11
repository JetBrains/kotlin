package soInlineIfConditionLambdaFalse

fun main(args: Array<String>) {
    bar {
        false
    }
}

inline fun bar(f: (Int) -> Boolean) {
    //Breakpoint!
    if (f(42)) {
        foo()
    }
    else {
        foo()
    }

    foo()
}

fun foo() {}

// STEP_OVER: 4