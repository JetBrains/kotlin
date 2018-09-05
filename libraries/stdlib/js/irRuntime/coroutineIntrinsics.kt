/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.experimental.intrinsics

import kotlin.coroutines.experimental.*

@SinceKotlin("1.1")
@kotlin.internal.InlineOnly
@Suppress("UNUSED_PARAMETER")
public suspend inline fun <T> suspendCoroutineOrReturn(crossinline block: (Continuation<T>) -> Any?): T =
    suspendCoroutineUninterceptedOrReturn { cont -> block(cont.intercepted()) }

/**
 * Obtains the current continuation instance inside suspend functions and either suspends
 * currently running coroutine or returns result immediately without suspension.
 *
 * Unlike [suspendCoroutineOrReturn] it does not intercept continuation.
 */
@SinceKotlin("1.2")
public suspend fun <T> suspendCoroutineUninterceptedOrReturn(block: (Continuation<T>) -> Any?): T =
    returnIfSuspended<T>(block(getContinuation<T>()))

/**
 * Intercept continuation with [ContinuationInterceptor].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun <T> Continuation<T>.intercepted() = normalizeContinuation<T>(this)

/**
 * This value is used as a return value of [suspendCoroutineOrReturn] `block` argument to state that
 * the execution was suspended and will not return any result immediately.
 */
@SinceKotlin("1.1")
public val COROUTINE_SUSPENDED: Any = Any()


@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
    completion: Continuation<T>
): Any? {
    val self_0 = this
    val cmpt_0 = completion
    return js("self_0.invoke(cmpt_0)")
    // TODO: use clean version once  function references is fixed
//    return (this as Function1<Continuation<T>, Any?>).invoke(completion)
}

@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public actual inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
    receiver: R,
    completion: Continuation<T>
): Any? {
    val self_0 = this
    val rec_0 = receiver
    val cmpt_0 = completion
    return js("self_0.invoke(rec_0, cmpt_0)")
    // TODO: use clean version once function references is fixed
//    return (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
}

@SinceKotlin("1.1")
public actual fun <R, T> (suspend R.() -> T).createCoroutineUnchecked(
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> {
    return if (this !is CoroutineImpl) {
        buildContinuationByInvokeCall(completion) {
            @Suppress("UNCHECKED_CAST") (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
        }
    } else {
        (create(receiver, completion) as CoroutineImpl).facade
    }
}

@SinceKotlin("1.1")
public actual fun <T> (suspend () -> T).createCoroutineUnchecked(
    completion: Continuation<T>
): Continuation<Unit> {
    return if (this !is CoroutineImpl) {
        buildContinuationByInvokeCall(completion) {
            @Suppress("UNCHECKED_CAST") (this as Function1<Continuation<T>, Any?>).invoke(completion)
        }
    } else {
        (create(completion) as CoroutineImpl).facade
    }
}

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

    return interceptContinuationIfNeeded(completion.context, continuation)
}