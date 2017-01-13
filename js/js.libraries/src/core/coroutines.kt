/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.coroutines

import kotlin.coroutines.intrinsics.*

/**
 * Creates coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
public fun <R, T> (suspend R.() -> T).createCoroutine(
        receiver: R,
        completion: Continuation<T>,
        dispatcher: ContinuationDispatcher? = null
): Continuation<Unit> = this.asDynamic().call(receiver, withDispatcher(completion, dispatcher), true).facade

/**
 * Starts coroutine with receiver type [R] and result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
public fun <R, T> (suspend R.() -> T).startCoroutine(
        receiver: R,
        completion: Continuation<T>,
        dispatcher: ContinuationDispatcher? = null
) {
    this.asDynamic().call(receiver, withDispatcher(completion, dispatcher))
}

/**
 * Creates coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
public fun <T> (suspend () -> T).createCoroutine(
        completion: Continuation<T>,
        dispatcher: ContinuationDispatcher? = null
): Continuation<Unit> = this.asDynamic()(withDispatcher(completion, dispatcher), true).facade

/**
 * Starts coroutine without receiver and with result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
public fun <T> (suspend  () -> T).startCoroutine(
        completion: Continuation<T>,
        dispatcher: ContinuationDispatcher? = null
) {
    this.asDynamic()(withDispatcher(completion, dispatcher))
}

/**
 * Obtains the current continuation instance inside suspend functions and suspends
 * currently running coroutine.
 *
 * In this function both [Continuation.resume] and [Continuation.resumeWithException] can be used either synchronously in
 * the same stack-frame where suspension function is run or asynchronously later in the same thread or
 * from a different thread of execution. Repeated invocation of any resume function produces [IllegalStateException].
 */
@SinceKotlin("1.1")
public suspend fun <T> suspendCoroutine(block: (Continuation<T>) -> Unit): T = suspendCoroutineOrReturn { c ->
    val safe = SafeContinuation(c)
    block(safe)
    safe.getResult()
}

// ------- internal stuff -------

internal interface DispatchedContinuation {
    val dispatcher: ContinuationDispatcher?
}

private fun <T> withDispatcher(completion: Continuation<T>, dispatcher: ContinuationDispatcher?): Continuation<T> {
    return if (dispatcher == null) {
        completion
    }
    else {
        object : Continuation<T> by completion, DispatchedContinuation {
            override val dispatcher = dispatcher
        }
    }
}

@JsName("CoroutineImpl")
internal abstract class CoroutineImpl(private val resultContinuation: Continuation<Any?>) : Continuation<Any?> {
    protected var state = 0
    protected var exceptionState = 0
    protected var result: Any? = null
    protected var exception: Throwable? = null
    protected var finallyPath: Array<Int>? = null
    private val continuationDispatcher = (resultContinuation as? DispatchedContinuation)?.dispatcher
    val facade: Continuation<Any?>

    init {
        facade = if (continuationDispatcher != null) {
            ContinuationFacade(this, continuationDispatcher)
        }
        else {
            this
        }
    }

    override fun resume(data: Any?) {
        result = data
        try {
            result = doResume()
            if (result != SUSPENDED_MARKER) {
                val data = result
                result = SUSPENDED_MARKER
                resultContinuation.resume(data)
            }
        }
        catch (e: Throwable) {
            resultContinuation.resumeWithException(e)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        state = exceptionState
        this.exception = exception
        try {
            result = doResume()
            if (result != SUSPENDED_MARKER) {
                val data = result
                result = SUSPENDED_MARKER
                resultContinuation.resume(data)
            }
        }
        catch (e: Throwable) {
            resultContinuation.resumeWithException(e)
        }
    }

    protected abstract fun doResume(): Any?
}

private class ContinuationFacade(val innerContinuation: Continuation<Any?>, val dispatcher: ContinuationDispatcher) : Continuation<Any?> {
    override fun resume(value: Any?) {
        if (!dispatcher.dispatchResume(value, innerContinuation)) {
            innerContinuation.resume(value)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        if (!dispatcher.dispatchResumeWithException(exception, innerContinuation)) {
            innerContinuation.resumeWithException(exception)
        }
    }
}

private val UNDECIDED: Any? = Any()
private val RESUMED: Any? = Any()
private class Fail(val exception: Throwable)

internal class SafeContinuation<in T> internal constructor(private val delegate: Continuation<T>) : Continuation<T> {
    private var result: Any? = UNDECIDED

    override fun resume(value: T) {
        when (result) {
            UNDECIDED -> {
                result = value
            }
            SUSPENDED_MARKER -> {
                result = RESUMED
                delegate.resume(value)
            }
            else -> {
                throw IllegalStateException("Already resumed")
            }
        }
    }

    override fun resumeWithException(exception: Throwable) {
        when (result) {
            UNDECIDED -> {
                result = Fail(exception)
            }
            SUSPENDED_MARKER -> {
                result = RESUMED
                delegate.resumeWithException(exception)
            }
            else -> {
                throw IllegalStateException("Already resumed")
            }
        }
    }

    internal fun getResult(): Any? {
        if (result == UNDECIDED) {
            result = SUSPENDED_MARKER
        }
        val result = this.result
        return when (result) {
            RESUMED -> {
                SUSPENDED_MARKER // already called continuation, indicate SUSPENDED upstream
            }
            is Fail -> {
                throw result.exception
            }
            else -> {
                result // either SUSPENDED or data
            }
        }
    }
}
