package soInlineIfConditionLambdaTrue

fun main(args: Array<String>) {
    bar {
        true
    }
}                                                   // 4

inline fun bar(f: (Int) -> Boolean) {
    //Breakpoint!
    if (f(42)) {                                    // 1
        foo()                                       // 2
    }
    else {
        foo()
    }

    foo()                                           // 3
}

fun foo() {}

// STEP_OVER: 4