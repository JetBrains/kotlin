// EXPECTED_REACHABLE_NODES: 490
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/regeneratedLambdaName.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun sameName(s: Long): Long {
    return call {
        s
    }
}

fun sameName(s: Int): Int {
    return call {
        s
    }
}

fun box(): String {
    val result = sameName(1)
    if (result != 1) return "fail1: ${result}"

    val result2 = sameName(2)
    if (result2 != 2) return "fail2: ${result2}"

    return "OK"
}


// FILE: test.kt
package test


inline fun <R> call(crossinline f: () -> R) : R {
    return {f()} ()
}
