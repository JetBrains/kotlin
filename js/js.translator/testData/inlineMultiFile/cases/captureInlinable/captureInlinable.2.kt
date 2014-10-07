/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/capture/captureInlinable.2.kt
 */

package test

import kotlin.InlineOption.*

inline fun <R> doWork(inlineOptions(ONLY_LOCAL_RETURN) job: ()-> R) : R {
    return notInline({job()})
}

fun <R> notInline(job: ()-> R) : R {
    return job()
}

