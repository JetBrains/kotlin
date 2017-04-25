// EXPECTED_REACHABLE_NODES: 496
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/lambdaTransformation/sameCaptured.1.kt
 */

// FILE: foo.kt
package foo

import test.*

fun testSameCaptured() : String {
    var result = 0;
    result = doWork({result+=1; result}, {result += 11; result})
    return if (result == 12) "OK" else "fail ${result}"
}

inline fun testSameCaptured(crossinline lambdaWithResultCaptured: () -> Unit) : String {
    var result = 1;
    result = doWork({result+=11; lambdaWithResultCaptured(); result})
    return if (result == 12) "OK" else "fail ${result}"
}

fun box(): String {
    if (testSameCaptured() != "OK") return "test1 : ${testSameCaptured()}"

    var result = 0;
    if (testSameCaptured{result += 1111} != "OK") return "test2 : ${testSameCaptured{result = 1111}}"

    return "OK"
}


// FILE: test.kt
package test


inline fun <R> doWork(crossinline job: ()-> R) : R {
    val k = 10;
    return notInline({k; job()})
}

inline fun <R> doWork(crossinline job: ()-> R, crossinline job2: () -> R) : R {
    val k = 10;
    return notInline({k; job(); job2()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}
