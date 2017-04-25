// EXPECTED_REACHABLE_NODES: 494
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/lambdaInLambda2.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun test1(prefix: String): String {
    var result = "fail"
    mfun {
        concat("start") {
            if (it.startsWith(prefix)) {
                result = "OK"
            }
        }
    }

    return result
}

fun box(): String {
    if (test1("start") != "OK") return "fail1"
    if (test1("nostart") != "fail") return "fail2"

    return "OK"
}


// FILE: test.kt
package test

inline fun <R> mfun(f: () -> R) {
    f()
}

fun concat(suffix: String, l: (s: String) -> Unit)  {
    l(suffix)
}