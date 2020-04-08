package stepOutInlinedLambdaArgument

fun main(args: Array<String>) {
    foo {
        //Breakpoint!
        test(1)
        test(2)
    }
    test(3)
}

inline fun foo(f: () -> Unit) {
    val a = 1
    f()
    val b = 2
}

fun test(i: Int) = 1

// STEP_OUT: 2