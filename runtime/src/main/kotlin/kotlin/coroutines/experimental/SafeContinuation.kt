/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.coroutines.experimental

import kotlin.coroutines.experimental.intrinsics.*

// Single-threaded continuation.
@PublishedApi
internal final actual class SafeContinuation<in T> internal actual constructor(
        private val delegate: Continuation<T>, initialResult: Any?) : Continuation<T> {

    @PublishedApi
    internal actual constructor(delegate: Continuation<T>) : this(delegate, UNDECIDED)

    actual override val context: CoroutineContext
        get() = delegate.context

    private var result: Any? = initialResult

    companion object {
        private val UNDECIDED: Any? = Any()
        private val RESUMED: Any? = Any()
    }

    private class Fail(val exception: Throwable)

    actual override fun resume(value: T) {
        when {
            result === UNDECIDED -> result = value
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resume(value)
            }
            else -> throw IllegalStateException("Already resumed")
        }
    }

    actual override fun resumeWithException(exception: Throwable) {
        when  {
            result === UNDECIDED -> result = Fail(exception)
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resumeWithException(exception)
            }
            else -> throw IllegalStateException("Already resumed")
        }
    }

    @PublishedApi
    internal actual fun getResult(): Any? {
        if (this.result === UNDECIDED) this.result = COROUTINE_SUSPENDED
        val result = this.result
        when {
            result === RESUMED -> return COROUTINE_SUSPENDED // already called continuation, indicate COROUTINE_SUSPENDED upstream
            result is Fail -> throw result.exception
            else -> return result // either COROUTINE_SUSPENDED or data
        }
    }
}