/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/tryCatchFinally/tryCatch.2.kt
 */

package foo

class My(val value: Int)

inline fun <T, R> T.perform(job: (T)-> R) : R {
    return job(this)
}

inline fun String.toInt2() : Int = parseInt(this)

class RuntimeExceptionWithValue(val value: String) : RuntimeException()