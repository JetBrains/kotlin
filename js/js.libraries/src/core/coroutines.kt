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
 * A strategy to intercept resumptions inside coroutine.
 * Interceptor may shift resumption into another execution frame by scheduling asynchronous execution
 * in this or another thread.
 */
@SinceKotlin("1.1")
public interface ResumeInterceptor {
    /**
     * Intercepts [Continuation.resume] invocation.
     * This function must either return `false` or return `true` and invoke `continuation.resume(data)` asynchronously.
     */
    public fun <P> interceptResume(data: P, continuation: Continuation<P>): Boolean = false

    /**
     * Intercepts [Continuation.resumeWithException] invocation.
     * This function must either return `false` or return `true` and invoke `continuation.resumeWithException(exception)` asynchronously.
     */
    public fun interceptResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean = false
}

/**
 * Creates coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The result of the coroutine's execution is provided via invocation of [resultHandler].
 */
@SinceKotlin("1.1")
public fun <R, T> (suspend R.() -> T).createCoroutine(
        receiver: R,
        resultHandler: Continuation<T>,
        resumeInterceptor: ResumeInterceptor? = null
): Continuation<Unit> = this.asDynamic().call(receiver, withInterceptor(resultHandler, resumeInterceptor))

/**
 * Starts coroutine with receiver type [R] and result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The result of the coroutine's execution is provided via invocation of [resultHandler].
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <R, T> (suspend R.() -> T).startCoroutine(
        receiver: R,
        resultHandler: Continuation<T>,
        resumeInterceptor: ResumeInterceptor? = null
) {
    createCoroutine(receiver, resultHandler, resumeInterceptor).resume(Unit)
}

/**
 * Creates coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The result of the coroutine's execution is provided via invocation of [resultHandler].
 * An optional [resumeInterceptor] may be specified to intercept resumes at suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
public fun <T> (suspend () -> T).createCoroutine(
        resultHandler: Continuation<T>,
        resumeInterceptor: ResumeInterceptor? = null
): Continuation<Unit> = this.asDynamic()(withInterceptor(resultHandler, resumeInterceptor))

/**
 * Starts coroutine without receiver and with result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The result of the coroutine's execution is provided via invocation of [resultHandler].
 * An optional [resumeInterceptor] may be specified to intercept resumes at suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <T> (suspend () -> T).startCoroutine(
        resultHandler: Continuation<T>,
        resumeInterceptor: ResumeInterceptor? = null
) {
    createCoroutine(resultHandler, resumeInterceptor).resume(Unit)
}

// ------- internal stuff -------

private fun <T> withInterceptor(resultHandler: Continuation<T>, resumeInterceptor: ResumeInterceptor?): Continuation<T> {
    val finalResumeInterceptor = resumeInterceptor ?: object : ResumeInterceptor {
        override fun <P> interceptResume(data: P, continuation: Continuation<P>) = false

        override fun interceptResumeWithException(exception: Throwable, continuation: Continuation<*>) = false
    }
    return object : Continuation<T> by resultHandler, ResumeInterceptor by finalResumeInterceptor {}
}

@JsName("CoroutineImpl")
internal abstract class CoroutineImpl(private val resultContinuation: Continuation<Any?>) : Continuation<Any?> {
    protected var state = 0
    protected var exceptionState = 0
    protected var result: Any? = null
    protected var exception: Throwable? = null
    protected var finallyPath: Array<Int>? = null
    private val resumeInterceptor = resultContinuation as? ResumeInterceptor

    override fun resume(data: Any?) {
        if (resumeInterceptor != null && (state and INTERCEPTING) == 0) {
            state = state or INTERCEPTING
            if (resumeInterceptor.interceptResume(data, this)) {
                state = state and INTERCEPTING.inv()
                return
            }
        }

        state = state and INTERCEPTING.inv()
        this.result = data
        try {
            val result = doResume()
            if (result != SUSPENDED) {
                resultContinuation.resume(result)
            }
        }
        catch (e: Throwable) {
            resultContinuation.resumeWithException(e)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        if (resumeInterceptor != null && (state and INTERCEPTING) == 0) {
            state = state or INTERCEPTING
            if (resumeInterceptor.interceptResumeWithException(exception, this)) {
                state = state and INTERCEPTING.inv()
                return
            }
        }

        state = exceptionState
        this.exception = exception
        try {
            val result = doResume()
            if (result != SUSPENDED) {
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