/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

@file:kotlin.jvm.JvmName("CoroutinesKt")
@file:kotlin.jvm.JvmVersion
package kotlin.coroutines.experimental

import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.experimental.jvm.internal.CoroutineImpl
import kotlin.coroutines.experimental.jvm.internal.interceptContinuationIfNeeded

/**
 * Creates coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <R, T> (suspend R.() -> T).createCoroutine(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit> =
    SafeContinuation(
        if (this !is CoroutineImpl)
            buildContinuationByInvokeCall(completion) {
                (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
            }
        else
            ((this as CoroutineImpl).create(receiver, completion) as CoroutineImpl).facade,
        COROUTINE_SUSPENDED
    )

/**
 * Starts coroutine with receiver type [R] and result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <R, T> (suspend R.() -> T).startCoroutine(
        receiver: R,
        completion: Continuation<T>
) {
    createCoroutine(receiver, completion).resume(Unit)
}

/**
 * Creates coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <T> (suspend () -> T).createCoroutine(
        completion: Continuation<T>
): Continuation<Unit> =
    SafeContinuation(
            if (this !is CoroutineImpl)
                buildContinuationByInvokeCall(completion) {
                    (this as Function1<Continuation<T>, Any?>).invoke(completion)
                }
            else
                ((this as CoroutineImpl).create(completion) as CoroutineImpl).facade,
            COROUTINE_SUSPENDED
    )

/**
 * Starts coroutine without receiver and with result type [T].
 * This function creates and start a new, fresh instance of suspendable computation every time it is invoked.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
public fun <T> (suspend  () -> T).startCoroutine(
        completion: Continuation<T>
) {
    createCoroutine(completion).resume(Unit)
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
public inline suspend fun <T> suspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T =
        suspendCoroutineOrReturn { c: Continuation<T> ->
            val safe = SafeContinuation(c)
            block(safe)
            safe.getResult()
        }

// INTERNAL DECLARATIONS

private inline fun <T> buildContinuationByInvokeCall(
        completion: Continuation<T>,
        crossinline block: () -> Any?
): Continuation<Unit> {
    val continuation =
            object : Continuation<Unit> {
                override val context: CoroutineContext
                    get() = completion.context

                override fun resume(value: Unit) {
                    processInvokeCallOnCoroutine(completion, block)
                }

                override fun resumeWithException(exception: Throwable) {
                    completion.resumeWithException(exception)
                }
            }

    return completion.context.interceptContinuationIfNeeded(continuation)
}

private inline fun processInvokeCallOnCoroutine(completion: Continuation<*>, block: () -> Any?) {
    try {
        val result = block()
        if (result !== COROUTINE_SUSPENDED) {
            @Suppress("UNCHECKED_CAST")
            (completion as Continuation<Any?>).resume(result)
        }
    } catch (t: Throwable) {
        completion.resumeWithException(t)
    }
}

private val UNDECIDED: Any? = Any()
private val RESUMED: Any? = Any()
private class Fail(val exception: Throwable)

@PublishedApi
internal class SafeContinuation<in T>
@PublishedApi internal constructor(
        private val delegate: Continuation<T>,
        initialResult: Any? = UNDECIDED
) : Continuation<T> {
    public override val context: CoroutineContext
        get() = delegate.context

    @Volatile
    private var result: Any? = initialResult

    companion object {
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        private val RESULT = AtomicReferenceFieldUpdater.newUpdater<SafeContinuation<*>, Any?>(
                SafeContinuation::class.java, Any::class.java as Class<Any?>, "result")
    }

    override fun resume(value: T) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when {
                result === UNDECIDED -> if (RESULT.compareAndSet(this, UNDECIDED, value)) return
                result === COROUTINE_SUSPENDED -> if (RESULT.compareAndSet(this, COROUTINE_SUSPENDED, RESUMED)) {
                    delegate.resume(value)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    override fun resumeWithException(exception: Throwable) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when  {
                result === UNDECIDED -> if (RESULT.compareAndSet(this, UNDECIDED, Fail(exception))) return
                result === COROUTINE_SUSPENDED -> if (RESULT.compareAndSet(this, COROUTINE_SUSPENDED, RESUMED)) {
                    delegate.resumeWithException(exception)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    @PublishedApi
    internal fun getResult(): Any? {
        var result = this.result // atomic read
        if (result === UNDECIDED) {
            if (RESULT.compareAndSet(this, UNDECIDED, COROUTINE_SUSPENDED)) return COROUTINE_SUSPENDED
            result = this.result // reread volatile var
        }
        when {
            result === RESUMED -> return COROUTINE_SUSPENDED // already called continuation, indicate COROUTINE_SUSPENDED upstream
            result is Fail -> throw result.exception
            else -> return result // either COROUTINE_SUSPENDED or data
        }
    }
}
