package throwableAsError

import kotlin.coroutines.*
import kotlin.native.internal.*
import kotlin.test.*

class ThrowableAsError : Throwable()

interface ThrowsThrowableAsError {
    @Throws(Throwable::class)
    fun throwError()
}

fun callAndCatchThrowableAsError(throwsThrowableAsError: ThrowsThrowableAsError): ThrowableAsError? {
    val frame = runtimeGetCurrentFrame()
    try {
        throwsThrowableAsError.throwError()
    } catch (e: ThrowableAsError) {
        assertEquals(frame, runtimeGetCurrentFrame())
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
