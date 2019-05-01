/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.intrinsics

import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * Starts an unintercepted coroutine without a receiver and with result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the latter case, the [completion] continuation is invoked when the coroutine completes with a result or an exception.
 *
 * The coroutine is started directly in the invoker's thread without going through the [ContinuationInterceptor] that might
 * be present in the completion's [CoroutineContext]. It is the invoker's responsibility to ensure that a proper invocation
 * context is established.
 *
 * This function is designed to be used from inside of [suspendCoroutineUninterceptedOrReturn] to resume the execution of the suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.3")
public expect inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
    completion: Continuation<T>
): Any?

/**
 * Starts an unintercepted coroutine with receiver type [R] and result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the latter case, the [completion] continuation is invoked when the coroutine completes with a result or an exception.
 *
 * The coroutine is started directly in the invoker's thread without going through the [ContinuationInterceptor] that might
 * be present in the completion's [CoroutineContext]. It is the invoker's responsibility to ensure that a proper invocation
 * context is established.
 *
 * This function is designed to be used from inside of [suspendCoroutineUninterceptedOrReturn] to resume the execution of the suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.3")
public expect inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
    receiver: R,
    completion: Continuation<T>
): Any?

@SinceKotlin("1.3")
public expect fun <T> (suspend () -> T).createCoroutineUnintercepted(
    completion: Continuation<T>
): Continuation<Unit>

@SinceKotlin("1.3")
public expect fun <R, T> (suspend R.() -> T).createCoroutineUnintercepted(
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit>

/**
 * Intercepts this continuation with [ContinuationInterceptor].
 *
 * This function shall be used on the immediate result of [createCoroutineUnintercepted] or [suspendCoroutineUninterceptedOrReturn],
 * in which case it checks for [ContinuationInterceptor] in the continuation's [context][Continuation.context],
 * invokes [ContinuationInterceptor.interceptContinuation], caches and returns the result.
 *
 * If this function is invoked on other [Continuation] instances it returns `this` continuation unchanged.
 */
@SinceKotlin("1.3")
public expect fun <T> Continuation<T>.intercepted(): Continuation<T>
