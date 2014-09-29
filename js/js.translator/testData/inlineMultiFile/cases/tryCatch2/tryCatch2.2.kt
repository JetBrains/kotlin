/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/tryCatchFinally/tryCatch2.2.kt
 */

package foo

class My(val value: Int)

inline fun <T, R> T.performWithFail(job: (T)-> R, failJob: (T) -> R): R {
    try {
        return job(this)
    } catch (e: RuntimeExceptionWithValue) {
        return failJob(this)
    }
}

inline fun <T, R> T.performWithFail2(job: (T)-> R, failJob: (e: RuntimeExceptionWithValue, T) -> R): R {
    try {
        return job(this)
    } catch (e: RuntimeExceptionWithValue) {
        return failJob(e, this)
    }
}

native object Number {
    fun parseInt(str: String): Int = noImpl
}

inline fun String.toInt2(): Int = parseInt(this)

class RuntimeExceptionWithValue(val value: String = "") : RuntimeException()