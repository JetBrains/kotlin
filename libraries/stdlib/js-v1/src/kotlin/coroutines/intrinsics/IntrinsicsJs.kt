/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.intrinsics

import kotlin.coroutines.*
import kotlin.internal.InlineOnly

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
@InlineOnly
public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
    completion: Continuation<T>
): Any? = this.asDynamic()(if (this !is CoroutineImpl) createSimpleCoroutineFromSuspendFunction(completion) else completion, false)

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
@InlineOnly
public actual inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
    receiver: R,
    completion: Continuation<T>
): Any? = this.asDynamic()(
    receiver, if (this !is CoroutineImpl) createSimpleCoroutineFromSuspendFunction(completion) else completion, false
)

@InlineOnly
internal actual inline fun <R, P, T> (suspend R.(P) -> T).startCoroutineUninterceptedOrReturn(
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? = this.asDynamic()(
    receiver, param, if (this !is CoroutineImpl) createSimpleCoroutineFromSuspendFunction(completion) else completion, false
)

/**
 * Creates unintercepted coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 *
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * This function returns unintercepted continuation.
 * Invocation of `resume(Unit)` starts coroutine immediately in the invoker's call stack without going through the
 * [ContinuationInterceptor] that might be present in the completion's [CoroutineContext].
 * It is the invoker's responsibility to ensure that a proper invocation context is established.
 * Note that [completion] of this function may get invoked in an arbitrary context.
 *
 * [Continuation.intercepted] can be used to acquire the intercepted continuation.
 * Invocation of `resume(Unit)` on intercepted continuation guarantees that execution of
 * both the coroutine and [completion] happens in the invocation context established by
 * [ContinuationInterceptor].
 *
 * Repeated invocation of any resume function on the resulting continuation corrupts the
 * state machine of the coroutine and may result in arbitrary behaviour or exception.
 */
@SinceKotlin("1.3")
public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(
    completion: Continuation<T>
): Continuation<Unit> =
    // Kotlin/JS suspend lambdas have an extra parameter `suspended`
    if (this.asDynamic().length == 2) {
        // When `suspended` is true the continuation is created, but not executed
        this.asDynamic()(completion, true)
    } else {
        createCoroutineFromSuspendFunction(completion) {
            this.asDynamic()(completion)
        }
    }

/**
 * Creates unintercepted coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 *
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * This function returns unintercepted continuation.
 * Invocation of `resume(Unit)` starts coroutine immediately in the invoker's call stack without going through the
 * [ContinuationInterceptor] that might be present in the completion's [CoroutineContext].
 * It is the invoker's responsibility to ensure that a proper invocation context is established.
 * Note that [completion] of this function may get invoked in an arbitrary context.
 *
 * [Continuation.intercepted] can be used to acquire the intercepted continuation.
 * Invocation of `resume(Unit)` on intercepted continuation guarantees that execution of
 * both the coroutine and [completion] happens in the invocation context established by
 * [ContinuationInterceptor].
 *
 * Repeated invocation of any resume function on the resulting continuation corrupts the
 * state machine of the coroutine and may result in arbitrary behaviour or exception.
 */
@SinceKotlin("1.3")
public actual fun <R, T> (suspend R.() -> T).createCoroutineUnintercepted(
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> =
    // Kotlin/JS suspend lambdas have an extra parameter `suspended`
    if (this.asDynamic().length == 3) {
        // When `suspended` is true the continuation is created, but not executed
        this.asDynamic()(receiver, completion, true)
    } else {
        createCoroutineFromSuspendFunction(completion) {
            this.asDynamic()(receiver, completion)
        }
    }

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
public actual fun <T> Continuation<T>.intercepted(): Continuation<T> =
    (this as? CoroutineImpl)?.intercepted() ?: this


private inline fun <T> createCoroutineFromSuspendFunction(
    completion: Continuation<T>,
    crossinline block: () -> Any?
): Continuation<Unit> {
    @Suppress("UNCHECKED_CAST")
    return object : CoroutineImpl(completion as Continuation<Any?>) {
        override fun doResume(): Any? {
            exception?.let { throw it }
            return block()
        }
    }
}

@PublishedApi
internal fun <T> createSimpleCoroutineFromSuspendFunction(
    completion: Continuation<T>
): CoroutineImpl = object : CoroutineImpl(@Suppress("UNCHECKED_CAST") (completion as Continuation<Any?>)) {
    override fun doResume(): Any? {
        if (exception != null) throw exception as Throwable
        return result
    }
}
