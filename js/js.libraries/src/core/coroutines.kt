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
): Continuation<Unit> = this.asDynamic().call(receiver, withDispatcher(completion, dispatcher)).facade

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
    createCoroutine(receiver, completion, dispatcher).resume(Unit)
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
): Continuation<Unit> = this.asDynamic()(withDispatcher(completion, dispatcher)).facade

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
    createCoroutine(completion, dispatcher).resume(Unit)
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
        this.result = data
        try {
            val result = doResume()
            if (result != CoroutineIntrinsics.SUSPENDED) {
                resultContinuation.resume(result)
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
            val result = doResume()
            if (result != CoroutineIntrinsics.SUSPENDED) {
                resultContinuation.resume(result)
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