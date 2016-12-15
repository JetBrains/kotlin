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
): Continuation<Unit> = this.asDynamic().call(receiver, withDispatcher(completion, dispatcher))

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
): Continuation<Unit> = this.asDynamic()(withDispatcher(completion, dispatcher))

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

private fun <T> withDispatcher(completion: Continuation<T>, dispatcher: ContinuationDispatcher?): Continuation<T> {
    val finalContinuationDispatcher = dispatcher ?: object : ContinuationDispatcher {
        override fun <T> dispatchResume(value: T, continuation: Continuation<T>) = false

        override fun dispatchResumeWithException(exception: Throwable, continuation: Continuation<*>) = false
    }
    return object : Continuation<T> by completion, ContinuationDispatcher by finalContinuationDispatcher {}
}

@JsName("CoroutineImpl")
internal abstract class CoroutineImpl(private val resultContinuation: Continuation<Any?>) : Continuation<Any?> {
    protected var state = 0
    protected var exceptionState = 0
    protected var result: Any? = null
    protected var exception: Throwable? = null
    protected var finallyPath: Array<Int>? = null
    private val continuationDispatcher = resultContinuation as? ContinuationDispatcher

    override fun resume(data: Any?) {
        if (continuationDispatcher != null && (state and INTERCEPTING) == 0) {
            state = state or INTERCEPTING
            if (continuationDispatcher.dispatchResume(data, this)) {
                state = state and INTERCEPTING.inv()
                return
            }
        }

        state = state and INTERCEPTING.inv()
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
        if (continuationDispatcher != null && (state and INTERCEPTING) == 0) {
            state = state or INTERCEPTING
            if (continuationDispatcher.dispatchResumeWithException(exception, this)) {
                state = state and INTERCEPTING.inv()
                return
            }
        }

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

private const val INTERCEPTING = 1 shl 31