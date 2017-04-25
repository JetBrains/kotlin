// EXPECTED_REACHABLE_NODES: 491
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/capture/captureInlinableAndOther.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun box(): String {
    val result = doWork({11})
    if (result != 11) return "test1: ${result}"

    val result2 = doWork({12; result+1})
    if (result2 != 12) return "test2: ${result2}"

    return "OK"
}


// FILE: test.kt
package test


inline fun <R> doWork(crossinline job: ()-> R) : R {
    val k = 10;
    return notInline({k; job()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}
