/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.experimental

import kotlin.coroutines.experimental.intrinsics.*

@JsName("CoroutineImpl")
internal abstract class CoroutineImpl(private val resultContinuation: Continuation<Any?>) : Continuation<Any?> {
    protected var state = 0
    protected var exceptionState = 0
    protected var result: Any? = null
    protected var exception: Throwable? = null
    protected var finallyPath: Array<Int>? = null

    public override val context: CoroutineContext = resultContinuation.context

    val facade: Continuation<Any?> = context[ContinuationInterceptor]?.interceptContinuation(this) ?: this

    override fun resume(value: Any?) {
        result = value
        doResumeWrapper()
    }

    override fun resumeWithException(exception: Throwable) {
        state = exceptionState
        this.exception = exception
        doResumeWrapper()
    }

    protected fun doResumeWrapper() {
        processBareContinuationResume(resultContinuation) { doResume() }
    }

    protected abstract fun doResume(): Any?
}

private val UNDECIDED: Any? = Any()
private val RESUMED: Any? = Any()

private class Fail(val exception: Throwable)

@PublishedApi
internal actual class SafeContinuation<in T>
internal actual constructor(
    private val delegate: Continuation<T>,
    initialResult: Any?
) : Continuation<T> {

    @PublishedApi
    internal actual constructor(delegate: Continuation<T>) : this(delegate, UNDECIDED)

    public actual override val context: CoroutineContext
        get() = delegate.context

    private var result: Any? = initialResult

    actual override fun resume(value: T) {
        when {
            result === UNDECIDED -> {
                result = value
            }
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resume(value)
            }
            else -> {
                throw IllegalStateException("Already resumed")
            }
        }
    }

    actual override fun resumeWithException(exception: Throwable) {
        when {
            result === UNDECIDED -> {
                result = Fail(exception)
            }
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resumeWithException(exception)
            }
            else -> {
                throw IllegalStateException("Already resumed")
            }
        }
    }

    @PublishedApi
    internal actual fun getResult(): Any? {
        if (result === UNDECIDED) {
            result = COROUTINE_SUSPENDED
        }
        val result = this.result
        return when {
            result === RESUMED -> {
                COROUTINE_SUSPENDED // already called continuation, indicate SUSPENDED upstream
            }
            result is Fail -> {
                throw result.exception
            }
            else -> {
                result // either SUSPENDED or data
            }
        }
    }
}
