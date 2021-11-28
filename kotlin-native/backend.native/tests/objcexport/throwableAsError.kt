package throwableAsError

import kotlin.coroutines.*

class ThrowableAsError : Throwable()

interface ThrowsThrowableAsError {
    @Throws(Throwable::class)
    fun throwError()
}

fun callAndCatchThrowableAsError(throwsThrowableAsError: ThrowsThrowableAsError): ThrowableAsError? {
    try {
        throwsThrowableAsError.throwError()
    } catch (e: ThrowableAsError) {
        return e
    }

    return null
}

interface ThrowsThrowableAsErrorSuspend {
    suspend fun throwError()
}

fun callAndCatchThrowableAsErrorSuspend(throwsThrowableAsErrorSuspend: ThrowsThrowableAsErrorSuspend): ThrowableAsError? {
    var throwable: ThrowableAsError? = null
    suspend {
        throwsThrowableAsErrorSuspend.throwError()
    }.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            throwable = result.exceptionOrNull() as? ThrowableAsError
        }
    })

    return throwable
}
