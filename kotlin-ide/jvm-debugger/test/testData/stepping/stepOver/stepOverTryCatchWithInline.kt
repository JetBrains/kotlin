package stepOverTryCatchWithInline

fun main(args: Array<String>) {
    try {
        bar()
    }
    catch(e: Exception) {                                                          // 13
        val a = 1                                                                  // 14
    }
}                                                                                  // 15

fun bar() {
    //Breakpoint!
    val prop = 1                                                                   // 1
    // Try
    try {                                                                          // 2
        foo { test(1) }                                                            // 3
    }
    catch(e: Exception) {
        foo { test(1) }
    }

    // Many catch clauses
    try {                                                                          // 4
        throw IllegalStateException()                                              // 5
    }
    catch(e: IllegalStateException) {                                              // 6
        foo { test(1) }                                                            // 7
    }
    catch(e: Exception) {
        foo { test(1) }
    }

    // exception in lambda
    try {                                                                          // 8
        foo { throw IllegalStateException() }                                      // 9
    }
    catch(e: Exception) {                                                          // 10
        foo { test(1) }                                                            // 11
    }

    // Exception without catch
    foo { throw IllegalStateException() }                                          // 12
    val prop2 = 1
}

inline fun foo(f: () -> Int): Int {
    val a = 1
    return f()
}

fun test(i: Int) = 1

// STEP_OVER: 15