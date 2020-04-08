package stepOverWhileWithInline

fun main(args: Array<String>) {
    //Breakpoint!
    var prop = 0                                                    // 1
    // inline on last line of while
    while(prop < 1) {                                               // 2 5
        prop++                                                      // 3
        foo { test(1) }                                             // 4
    }

    // inline call inside while
    while(prop < 2) {                                               // 6 9
        foo { test(1) }                                             // 7
        prop++                                                      // 8
    }

    do {
        prop++                                                      // 10
        foo { test(1) }                                             // 11
    } while(prop < 3)                                               // 12

    do {
        foo { test(1) }                                             // 13
        prop++                                                      // 14
    } while(prop < 4)                                               // 15
}                                                                   // 16

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = i

// STEP_OVER: 16