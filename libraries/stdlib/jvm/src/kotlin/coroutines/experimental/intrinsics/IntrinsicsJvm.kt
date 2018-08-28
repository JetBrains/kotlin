/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("IntrinsicsKt")
@file:kotlin.jvm.JvmMultifileClass

package kotlin.coroutines.experimental.intrinsics

import kotlin.coroutines.experimental.*

/**
 * Starts unintercepted coroutine without receiver and with result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 * This function is designed to be used from inside of [suspendCoroutineOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
    completion: Continuation<T>
): Any? = (this as Function1<Continuation<T>, Any?>).invoke(completion)

/**
 * Starts unintercepted coroutine with receiver type [R] and result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 * This function is designed to be used from inside of [suspendCoroutineOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public actual inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
    receiver: R,
    completion: Continuation<T>
): Any? = (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)


// JVM declarations

/**
 * Creates a coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 *
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * This function is _unchecked_. Repeated invocation of any resume function on the resulting continuation corrupts the
 * state machine of the coroutine and may result in arbitrary behaviour or exception.
 */
@SinceKotlin("1.1")
public actual fun <T> (suspend () -> T).createCoroutineUnchecked(
    completion: Continuation<T>
): Continuation<Unit> =
    if (this !is kotlin.coroutines.experimental.jvm.internal.CoroutineImpl)
        buildContinuationByInvokeCall(completion) {
            @Suppress("UNCHECKED_CAST")
            (this as Function1<Continuation<T>, Any?>).invoke(completion)
        }
    else
        (this.create(completion) as kotlin.coroutines.experimental.jvm.internal.CoroutineImpl).facade

/**
 * Creates a coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 *
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * This function is _unchecked_. Repeated invocation of any resume function on the resulting continuation corrupts the
 * state machine of the coroutine and may result in arbitrary behaviour or exception.
 */
@SinceKotlin("1.1")
public actual fun <R, T> (suspend R.() -> T).createCoroutineUnchecked(
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> =
    if (this !is kotlin.coroutines.experimental.jvm.internal.CoroutineImpl)
        buildContinuationByInvokeCall(completion) {
            @Suppress("UNCHECKED_CAST")
            (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
        }
    else
        (this.create(receiver, completion) as kotlin.coroutines.experimental.jvm.internal.CoroutineImpl).facade

// INTERNAL DEFINITIONS

private inline fun <T> buildContinuationByInvokeCall(
    completion: Continuation<T>,
    crossinline block: () -> Any?
): Continuation<Unit> {
    val continuation =
        object : Continuation<Unit> {
            override val context: CoroutineContext
                get() = completion.context

            override fun resume(value: Unit) {
                processBareContinuationResume(completion, block)
            }

            override fun resumeWithException(exception: Throwable) {
                completion.resumeWithException(exception)
            }
        }

    return kotlin.coroutines.experimental.jvm.internal.interceptContinuationIfNeeded(completion.context, continuation)
}

/**
 * This value is used as a return value of [suspendCoroutineOrReturn] `block` argument to state that
 * the execution was suspended and will not return any result immediately.
 */
@SinceKotlin("1.1")
public actual val COROUTINE_SUSPENDED: Any get() = CoroutineSingletons.COROUTINE_SUSPENDED

// Using enum here ensures two important properties:
//  1. It makes SafeContinuation serializable with all kinds of serialization frameworks (since all of them natively support enums)
//  2. It improves debugging experience, since you clearly see toString() value of those objects and what package they come from
private enum class CoroutineSingletons {
    COROUTINE_SUSPENDED
}
