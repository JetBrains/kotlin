

package foo

class My(val value: Int)

inline fun <T, R> T.performWithFinally(job: (T)-> R, finallyFun: (T) -> R) : R {
    try {
        job(this)
    } finally {
        return finallyFun(this)
    }
}

inline fun <T, R> T.performWithFailFinally(job: (T)-> R, failJob : (e: RuntimeException, T) -> R, finallyFun: (T) -> R) : R {
    try {
        job(this)
    } catch (e: RuntimeException) {
        failJob(e, this)
    } finally {
        return finallyFun(this)
    }
}

inline fun String.toInt2() : Int = parseInt(this)