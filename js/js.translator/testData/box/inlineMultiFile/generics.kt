// EXPECTED_REACHABLE_NODES: 488
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/capture/generics.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun test1(s: Int): String {
    var result = "OK"
    result = mfun(s) { a ->
        result + doSmth(s) + doSmth(a)
    }

    return result
}

fun box(): String {
    val result = test1(11)
    if (result != "OK1111") return "fail1: ${result}"

    return "OK"
}


// FILE: test.kt
package test

inline fun <T, R> mfun(arg: T, f: (T) -> R) : R {
    return f(arg)
}

inline fun <T> doSmth(a: T): String {
    return a.toString()
}