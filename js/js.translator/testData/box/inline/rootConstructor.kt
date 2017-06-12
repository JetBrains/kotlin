// EXPECTED_REACHABLE_NODES: 491
/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/simple/rootConstructor.1.kt
 */

package foo


inline fun <R> doWork(crossinline job: ()-> R) : R {
    return notInline({job()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}

val s = doWork({11})

fun box(): String {
    if (s != 11) return "test1: ${s}"

    return "OK"
}