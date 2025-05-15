/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.coroutines.intrinsics

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.*
import kotlin.coroutines.native.internal.*
import kotlin.internal.InlineOnly
import kotlin.native.internal.*

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
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
        completion: Continuation<T>
): Any? {
    val wrappedCompletion = wrapWithContinuationImpl(completion)
    val function = this as? Function1<Continuation<T>, Any?>
    return if (function == null)
        startCoroutineUninterceptedOrReturnFallback(this, wrappedCompletion)
    else
        function.invoke(wrappedCompletion)
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <T> startCoroutineUninterceptedOrReturnFallback(
        function: suspend () -> T,
        completion: Continuation<T>
): Any? {
    // Unlike `function`, `wrapper` class is generated and lowered entirely by Kotlin compiler,
    // so the cast below will succeed.
    val wrapper: suspend () -> T = { function() }
    return (wrapper as Function1<Continuation<T>, Any?>).invoke(completion)
}

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
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public actual inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
        receiver: R,
        completion: Continuation<T>
): Any? {
    val wrappedCompletion = wrapWithContinuationImpl(completion)
    val function = this as? Function2<R, Continuation<T>, Any?>
    return if (function == null)
        startCoroutineUninterceptedOrReturnFallback(this, receiver, wrappedCompletion)
    else
        function.invoke(receiver, wrappedCompletion)
}

@Suppress("UNCHECKED_CAST")
@PublishedApi
internal fun <R, T> startCoroutineUninterceptedOrReturnFallback(
        function: suspend R.() -> T,
        receiver: R,
        completion: Continuation<T>
): Any? {
    // Unlike `function`, `wrapper` class is generated and lowered entirely by Kotlin compiler,
    // so the cast below will succeed.
    val wrapper: suspend R.() -> T = { this.function() }
    return (wrapper as Function2<R, Continuation<T>, Any?>).invoke(receiver, completion)
}

@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
internal actual inline fun <R, P, T> (suspend R.(P) -> T).startCoroutineUninterceptedOrReturn(
        receiver: R,
        param: P,
        completion: Continuation<T>
): Any? {
    val wrappedCompletion = wrapWithContinuationImpl(completion)
    val function = this as? Function3<R, P, Continuation<T>, Any?>
    return if (function == null)
        startCoroutineUninterceptedOrReturnFallback(this, receiver, param, wrappedCompletion)
    else
        function.invoke(receiver, param, wrappedCompletion)
}

@Suppress("UNCHECKED_CAST")
internal fun <R, P, T> startCoroutineUninterceptedOrReturnFallback(
        function: suspend R.(P) -> T,
        receiver: R,
        param: P,
        completion: Continuation<T>
): Any? {
    // Unlike `function`, `wrapper` class is generated and lowered entirely by Kotlin compiler,
    // so the cast below will succeed.
    val wrapper: suspend R.(P) -> T = { this.function(it) }
    return (wrapper as Function3<R, P, Continuation<T>, Any?>).invoke(receiver, param, completion)
}

private object CoroutineSuspendedMarker


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
@Suppress("UNCHECKED_CAST")
public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(
        completion: Continuation<T>
): Continuation<Unit> {
    val probeCompletion = probeCoroutineCreated(completion)
    return if (this is BaseContinuationImpl)
        create(probeCompletion)
    else
        createCoroutineFromSuspendFunction(probeCompletion) {
            this.startCoroutineUninterceptedOrReturn(it)
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
@Suppress("UNCHECKED_CAST")
public actual fun <R, T> (suspend R.() -> T).createCoroutineUnintercepted(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit> {
    val probeCompletion = probeCoroutineCreated(completion)
    return if (this is BaseContinuationImpl)
        create(receiver, probeCompletion)
    else {
        createCoroutineFromSuspendFunction(probeCompletion) {
            this.startCoroutineUninterceptedOrReturn(receiver, it)
        }
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
@Suppress("UNCHECKED_CAST")
private inline fun <T> createCoroutineFromSuspendFunction(
        completion: Continuation<T>,
        crossinline block: (Continuation<T>) -> Any?
): Continuation<Unit> {
    val context = completion.context
    // label == 0 when coroutine is not started yet (initially) or label == 1 when it was
    return if (context === EmptyCoroutineContext)
        object : RestrictedContinuationImpl(completion as Continuation<Any?>) {
            private var label = 0

            override fun invokeSuspend(result: Result<Any?>): Any? =
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

            override fun invokeSuspend(result: Result<Any?>): Any? =
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

/**
 * This function creates continuation suitable for passing as implicit argument to suspend functions.
 * The continuation calls [callback] and then delegates to [completion].
 *
 * The result is [ContinuationImpl] because that is an expectation of all coroutines machinery.
 *
 * It can be thought as a state machine of
 * ```
 * suspend fun foo() {
 *     val result = runCatching { <suspended here> }
 *     callback(result)
 * }
 * ```
 */
@Suppress("UNCHECKED_CAST")
internal inline fun createContinuationArgumentFromCallback(
        completion: Continuation<Unit>,
        crossinline callback: (Result<Any?>) -> Unit
): Continuation<Any?> = object : ContinuationImpl(completion as Continuation<Any?>) {
    private var invoked = false

    override fun invokeSuspend(result: Result<Any?>): Any? {
        if (invoked) error("This coroutine had already completed")
        invoked = true

        callback(result)

        return Unit // Not suspended.
    }
}

@PublishedApi
internal fun <T> wrapWithContinuationImpl(completion: Continuation<T>): Continuation<T> =
        createSimpleCoroutineForSuspendFunction(probeCoroutineCreated(completion))

/**
 * This function is used when [startCoroutineUninterceptedOrReturn] encounters suspending lambda that does not extend BaseContinuationImpl.
 *
 * It happens in two cases: callable reference to suspending function or tail-call lambdas.
 *
 * This function is the same as above, but does not run lambda itself - the caller is expected to call [invoke] manually.
 */
@Suppress("UNCHECKED_CAST")
private fun <T> createSimpleCoroutineForSuspendFunction(
        completion: Continuation<T>
): Continuation<T> {
    val context = completion.context
    return if (context === EmptyCoroutineContext)
        object : RestrictedContinuationImpl(completion as Continuation<Any?>) {
            override fun invokeSuspend(result: Result<Any?>): Any? {
                return result.getOrThrow()
            }
        }
    else
        object : ContinuationImpl(completion as Continuation<Any?>, context) {
            override fun invokeSuspend(result: Result<Any?>): Any? {
                return result.getOrThrow()
            }
        }
}

/**
 * Obtains the current continuation instance inside suspend functions and either suspends
 * currently running coroutine or returns result immediately without suspension.
 *
 * If the [block] returns the special [COROUTINE_SUSPENDED] value, it means that suspend function did suspend the execution and will
 * not return any result immediately. In this case, the [Continuation] provided to the [block] shall be
 * resumed by invoking [Continuation.resumeWith] at some moment in the
 * future when the result becomes available to resume the computation.
 *
 * Otherwise, the return value of the [block] must have a type assignable to [T] and represents the result of this suspend function.
 * It means that the execution was not suspended and the [Continuation] provided to the [block] shall not be invoked.
 * As the result type of the [block] is declared as `Any?` and cannot be correctly type-checked,
 * its proper return type remains on the conscience of the suspend function's author.
 *
 * Invocation of [Continuation.resumeWith] resumes coroutine directly in the invoker's thread without going through the
 * [ContinuationInterceptor] that might be present in the coroutine's [CoroutineContext].
 * It is the invoker's responsibility to ensure that a proper invocation context is established.
 * [Continuation.intercepted] can be used to acquire the intercepted continuation.
 *
 * Note that it is not recommended to call either [Continuation.resume] nor [Continuation.resumeWithException] functions synchronously
 * in the same stackframe where suspension function is run. Use [suspendCoroutine] as a safer way to obtain current
 * continuation instance.
 */
@SinceKotlin("1.3")
@InlineOnly
@Suppress("WRONG_INVOCATION_KIND", "UNUSED_PARAMETER")
public actual suspend inline fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return returnIfSuspended<T>(block(getContinuation<T>()))
}
