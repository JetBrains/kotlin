package stepOverForWithInline

fun main(args: Array<String>) {
    //Breakpoint!
    var prop = 0
    // inline on last line in for
    for(i in 0..1) {
        prop++
        foo { test(1) }
    }
}

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = i

// STEP_OVER: 9