/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("IntrinsicsKt")
@file:kotlin.jvm.JvmMultifileClass
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNCHECKED_CAST")

package kotlin.coroutines.intrinsics

import kotlin.coroutines.*
import kotlin.coroutines.jvm.internal.*
import kotlin.internal.InlineOnly

/**
 * Starts unintercepted coroutine without receiver and with result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * The coroutine is started directly in the invoker's thread without going through the [ContinuationInterceptor] that might
 * be present in the completion's [CoroutineContext]. It is invoker's responsibility to ensure that the proper invocation
 * context is established.
 *
 * This function is designed to be used from inside of [suspendCoroutineUninterceptedOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.3")
@InlineOnly
public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
    completion: Continuation<T>
): Any? = (this as Function1<Continuation<T>, Any?>).invoke(completion)

/**
 * Starts unintercepted coroutine with receiver type [R] and result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the later case, the [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * The coroutine is started directly in the invoker's thread without going through the [ContinuationInterceptor] that might
 * be present in the completion's [CoroutineContext]. It is invoker's responsibility to ensure that the proper invocation
 * context is established.
 *
 * This function is designed to be used from inside of [suspendCoroutineUninterceptedOrReturn] to resume the execution of suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.3")
@InlineOnly
public actual inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
    receiver: R,
    completion: Continuation<T>
): Any? = (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)


// JVM declarations

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
 * It is invoker's responsibility to ensure that the proper invocation context is established.
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
): Continuation<Unit> {
    val probeCompletion = probeCoroutineCreated(completion)
    return if (this is BaseContinuationImpl)
        create(probeCompletion)
    else
        createCoroutineFromSuspendFunction(probeCompletion) {
            (this as Function1<Continuation<T>, Any?>).invoke(it)
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
 * It is invoker's responsibility to ensure that the proper invocation context is established.
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
): Continuation<Unit> {
    val probeCompletion = probeCoroutineCreated(completion)
    return if (this is BaseContinuationImpl)
        create(receiver, probeCompletion)
    else {
        createCoroutineFromSuspendFunction(probeCompletion) {
            (this as Function2<R, Continuation<T>, Any?>).invoke(receiver, it)
        }
    }
}

/**
 * Intercepts this continuation with [ContinuationInterceptor].
 *
 * This function shall be used on the immediate result of [createCoroutineUnintercepted] or [suspendCoroutineUninterceptedOrReturn],
 * in which case it checks for [ContinuationInterceptor] in the continuation's [context][Continuation.context],
 * invokes [ContinuationInterceptor.interceptContinuation], caches and returns result.
 *
 * If this function is invoked on other [Continuation] instances it returns `this` continuation unchanged.
 */
@SinceKotlin("1.3")
public actual fun <T> Continuation<T>.intercepted(): Continuation<T> =
    (this as? ContinuationImpl)?.intercepted() ?: this

// INTERNAL DEFINITIONS

/**
 * This function is used when [createCoroutineUnintercepted] encounters suspending lambda that does not extend BaseContinuationImpl.
 *
 * It happens in two cases:
 *   1. Callable reference to suspending function,
 *   2. Suspending function reference implemented by Java code.
 *
 * We must wrap it into an instance that extends [BaseContinuationImpl], because that is an expectation of all coroutines machinery.
 * As an optimization we use lighter-weight [RestrictedContinuationImpl] base class (it has less fields) if the context is
 * [EmptyCoroutineContext], and a full-blown [ContinuationImpl] class otherwise.
 *
 * The instance of [BaseContinuationImpl] is passed to the [block] so that it can be passed to the corresponding invocation.
 */
@SinceKotlin("1.3")
private inline fun <T> createCoroutineFromSuspendFunction(
    completion: Continuation<T>,
    crossinline block: (Continuation<T>) -> Any?
): Continuation<Unit> {
    val context = completion.context
    // label == 0 when coroutine is not started yet (initially) or label == 1 when it was
    return if (context === EmptyCoroutineContext)
        object : RestrictedContinuationImpl(completion as Continuation<Any?>) {
            private var label = 0

            override fun invokeSuspend(result: SuccessOrFailure<Any?>): Any? =
                when (label) {
                    0 -> {
                        label = 1
                        result.getOrThrow() // Rethrow exception if trying to start with exception (will be caught by BaseContinuationImpl.resumeWith
                        block(this) // run the block, may return or suspend
                    }
                    1 -> {
                        label = 2
                        result.getOrThrow() // this is the result if the block had suspended
                    }
                    else -> error("This coroutine had already completed")
                }
        }
    else
        object : ContinuationImpl(completion as Continuation<Any?>, context) {
            private var label = 0

            override fun invokeSuspend(result: SuccessOrFailure<Any?>): Any? =
                when (label) {
                    0 -> {
                        label = 1
                        result.getOrThrow() // Rethrow exception if trying to start with exception (will be caught by BaseContinuationImpl.resumeWith
                        block(this) // run the block, may return or suspend
                    }
                    1 -> {
                        label = 2
                        result.getOrThrow() // this is the result if the block had suspended
                    }
                    else -> error("This coroutine had already completed")
                }
        }
}
