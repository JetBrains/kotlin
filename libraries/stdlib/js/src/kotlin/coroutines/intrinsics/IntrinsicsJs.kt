/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "UNCHECKED_CAST")

package kotlin.coroutines.intrinsics

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.coroutines.*
import kotlin.internal.InlineOnly

/**
 * Invoke 'invoke' method of suspend super type
 * Because callable references translated with local classes,
 * necessary to call it in special way, not in synamic way
 */
@Suppress("UNUSED_PARAMETER", "unused")
@PublishedApi
internal fun <T> (suspend () -> T).invokeSuspendSuperType(
    completion: Continuation<T>
): Any? {
    throw NotImplementedError("It is intrinsic method")
}

/**
 * Invoke 'invoke' method of suspend super type with receiver
 * Because callable references translated with local classes,
 * necessary to call it in special way, not in synamic way
 */
@Suppress("UNUSED_PARAMETER", "unused")
@PublishedApi
internal fun <R, T> (suspend R.() -> T).invokeSuspendSuperTypeWithReceiver(
    receiver: R,
    completion: Continuation<T>
): Any? {
    throw NotImplementedError("It is intrinsic method")
}

/**
 * Invoke 'invoke' method of suspend super type with receiver and param
 * Because callable references translated with local classes,
 * necessary to call it in special way, not in synamic way
 */
@Suppress("UNUSED_PARAMETER", "unused")
@PublishedApi
internal fun <R, P, T> (suspend R.(P) -> T).invokeSuspendSuperTypeWithReceiverAndParam(
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? {
    throw NotImplementedError("It is intrinsic method")
}

/**
 * Starts unintercepted coroutine without receiver and with result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the latter case, the [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * The coroutine is started directly in the invoker's thread without going through the [ContinuationInterceptor] that might
 * be present in the completion's [CoroutineContext]. It is the invoker's responsibility to ensure that a proper invocation
 * context is established.
 *
 * This function is designed to be used from inside of [suspendCoroutineUninterceptedOrReturn] to resume the execution of a suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.3")
@InlineOnly
public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
    completion: Continuation<T>
): Any? = startCoroutineUninterceptedOrReturnNonGeneratorVersion(completion)

@PublishedApi
internal fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturnNonGeneratorVersion(
    completion: Continuation<T>
): Any? {
    // Wrap with CoroutineImpl, otherwise the coroutine will not be interceptable. See KT-55869
    val wrappedCompletion = if (completion !is InterceptedCoroutine)
        createSimpleCoroutineForSuspendFunction(completion)
    else
        completion
    val a = this.asDynamic()
    return if (jsTypeOf(a) == "function") a(wrappedCompletion)
    else this.invokeSuspendSuperType(wrappedCompletion)
}

/**
 * Starts unintercepted coroutine with receiver type [R] and result type [T] and executes it until its first suspension.
 * Returns the result of the coroutine or throws its exception if it does not suspend or [COROUTINE_SUSPENDED] if it suspends.
 * In the latter case, the [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * The coroutine is started directly in the invoker's thread without going through the [ContinuationInterceptor] that might
 * be present in the completion's [CoroutineContext]. It is the invoker's responsibility to ensure that a proper invocation
 * context is established.
 *
 * This function is designed to be used from inside of [suspendCoroutineUninterceptedOrReturn] to resume the execution of a suspended
 * coroutine using a reference to the suspending function.
 */
@SinceKotlin("1.3")
@InlineOnly
public actual inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
    receiver: R,
    completion: Continuation<T>
): Any? = startCoroutineUninterceptedOrReturnNonGeneratorVersion(receiver, completion)

@PublishedApi
internal fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturnNonGeneratorVersion(
    receiver: R,
    completion: Continuation<T>
): Any? {
    // Wrap with CoroutineImpl, otherwise the coroutine will not be interceptable. See KT-55869
    val wrappedCompletion = if (completion !is InterceptedCoroutine)
        createSimpleCoroutineForSuspendFunction(completion)
    else
        completion
    val a = this.asDynamic()
    return if (jsTypeOf(a) == "function") a(receiver, wrappedCompletion)
    else this.invokeSuspendSuperTypeWithReceiver(receiver, wrappedCompletion)
}

@InlineOnly
internal actual inline fun <R, P, T> (suspend R.(P) -> T).startCoroutineUninterceptedOrReturn(
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? = startCoroutineUninterceptedOrReturnNonGeneratorVersion(receiver, param, completion)

@PublishedApi
internal fun <R, P, T> (suspend R.(P) -> T).startCoroutineUninterceptedOrReturnNonGeneratorVersion(
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? {
    // Wrap with CoroutineImpl, otherwise the coroutine will not be interceptable. See KT-55869
    val wrappedCompletion = if (completion !is InterceptedCoroutine)
        createSimpleCoroutineForSuspendFunction(completion)
    else
        completion
    val a = this.asDynamic()
    return if (jsTypeOf(a) == "function") a(receiver, param, wrappedCompletion)
    else this.invokeSuspendSuperTypeWithReceiverAndParam(receiver, param, wrappedCompletion)
}

/**
 * Creates unintercepted coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 *
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * This function returns unintercepted continuation.
 * Invocation of `resume(Unit)` starts coroutine directly in the invoker's thread without going through the
 * [ContinuationInterceptor] that might be present in the completion's [CoroutineContext].
 * It is the invoker's responsibility to ensure that a proper invocation context is established.
 * [Continuation.intercepted] can be used to acquire the intercepted continuation.
 *
 * Repeated invocation of any resume function on the resulting continuation corrupts the
 * state machine of the coroutine and may result in arbitrary behaviour or exception.
 */
@SinceKotlin("1.3")
public actual fun <T> (suspend () -> T).createCoroutineUnintercepted(
    completion: Continuation<T>
): Continuation<Unit> =
    createCoroutineFromSuspendFunction(completion) {
        val a = this.asDynamic()
        if (jsTypeOf(a) == "function") a(completion)
        else this.invokeSuspendSuperType(completion)
    }

/**
 * Creates unintercepted coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 *
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result or exception.
 *
 * This function returns unintercepted continuation.
 * Invocation of `resume(Unit)` starts coroutine directly in the invoker's thread without going through the
 * [ContinuationInterceptor] that might be present in the completion's [CoroutineContext].
 * It is the invoker's responsibility to ensure that a proper invocation context is established.
 * [Continuation.intercepted] can be used to acquire the intercepted continuation.
 *
 * Repeated invocation of any resume function on the resulting continuation corrupts the
 * state machine of the coroutine and may result in arbitrary behaviour or exception.
 */
@SinceKotlin("1.3")
public actual fun <R, T> (suspend R.() -> T).createCoroutineUnintercepted(
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> =
    createCoroutineFromSuspendFunction(completion) {
        val a = this.asDynamic()
        if (jsTypeOf(a) == "function") a(receiver, completion)
        else this.invokeSuspendSuperTypeWithReceiver(receiver, completion)
    }

/**
 * Intercepts this continuation with [ContinuationInterceptor].
 */
@SinceKotlin("1.3")
public actual fun <T> Continuation<T>.intercepted(): Continuation<T> =
    (this as? InterceptedCoroutine)?.intercepted() ?: this

private inline fun <T> createCoroutineFromSuspendFunction(
    completion: Continuation<T>,
    crossinline block: () -> Any?
): Continuation<Unit> {
    return object : CoroutineImpl(completion as Continuation<Any?>) {
        override fun doResume(): Any? {
            if (exception != null) throw exception
            return block()
        }
    }
}

private fun <T> createSimpleCoroutineForSuspendFunction(
    completion: Continuation<T>,
): Continuation<T> {
    return object : CoroutineImpl(completion as Continuation<Any?>) {
        override fun doResume(): Any? {
            @Suppress("UnsafeCastFromDynamic")
            if (exception != null) throw exception
            return result
        }
    }
}

@InlineOnly
internal inline fun <T> createCoroutineFromGeneratorFunction(
    completion: Continuation<T>,
    crossinline generatorFunction: (Continuation<T>) -> dynamic,
): Continuation<Any?> {
    val continuation = GeneratorCoroutineImpl(completion.unsafeCast<Continuation<Any?>>())
    continuation.addNewIterator(dummyGenerator(COROUTINE_SUSPENDED) { generatorFunction(continuation) })
    return continuation
}

@InlineOnly
internal inline fun <T> startCoroutineFromGeneratorFunction(
    completion: Continuation<T>,
    crossinline generatorFunction: (Continuation<T>) -> dynamic,
): Any? {
    val continuation = GeneratorCoroutineImpl(completion.unsafeCast<Continuation<Any?>>())
    continuation.isRunning = true
    val result = generatorFunction(continuation)
    continuation.isRunning = false
    if (continuation.shouldResumeImmediately()) continuation.resume(result)
    return result
}

internal fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturnGeneratorVersion(
    completion: Continuation<T>
): Any? = startCoroutineFromGeneratorFunction(completion) {
    val a = asDynamic()
    if (jsTypeOf(a) === "function") a(it)
    else invokeSuspendSuperType(it)
}

internal fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturnGeneratorVersion(
    receiver: R,
    completion: Continuation<T>
): Any? = startCoroutineFromGeneratorFunction(completion) {
    val a = asDynamic()
    if (jsTypeOf(a) === "function") a(receiver, it)
    else invokeSuspendSuperTypeWithReceiver(receiver, it)
}

internal fun <R, P, T> (suspend R.(P) -> T).startCoroutineUninterceptedOrReturnGeneratorVersion(
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? = startCoroutineFromGeneratorFunction(completion) {
    val a = asDynamic()
    if (jsTypeOf(a) === "function") a(receiver, param, it)
    else invokeSuspendSuperTypeWithReceiverAndParam(receiver, param, it)
}

internal fun <T> (suspend () -> T).createCoroutineUninterceptedGeneratorVersion(
    completion: Continuation<T>
): Continuation<Any?> =
    createCoroutineFromGeneratorFunction(completion) {
        val a = asDynamic()
        if (jsTypeOf(a) === "function") a(it)
        else invokeSuspendSuperType(it)
    }

internal fun <R, T> (suspend R.() -> T).createCoroutineUninterceptedGeneratorVersion(
    receiver: R,
    completion: Continuation<T>
): Continuation<Unit> =
    createCoroutineFromGeneratorFunction(completion) {
        val a = asDynamic()
        if (jsTypeOf(a) === "function") a(receiver, it)
        else invokeSuspendSuperTypeWithReceiver(receiver, it)
    }


internal fun <R, T, P> (suspend R.(P) -> T).createCoroutineUninterceptedGeneratorVersion(
    receiver: R,
    param: P,
    completion: Continuation<T>
): Continuation<Unit> =
    createCoroutineFromGeneratorFunction(completion) {
        val a = asDynamic()
        if (jsTypeOf(a) === "function") a(receiver, param, it)
        else invokeSuspendSuperTypeWithReceiverAndParam(receiver, param, it)
    }


internal fun suspendOrReturn(generator: (continuation: Continuation<Any?>) -> dynamic, continuation: Continuation<Any?>): Any? {
    val generatorCoroutineImpl = if (continuation.asDynamic().constructor === GeneratorCoroutineImpl::class.js) {
        continuation.unsafeCast<GeneratorCoroutineImpl>()
    } else {
        GeneratorCoroutineImpl(continuation)
    }

    val value = generator(generatorCoroutineImpl)

    if (!isGeneratorSuspendStep(value)) return value

    val iterator = value.unsafeCast<JsIterator<Any?>>()

    generatorCoroutineImpl.addNewIterator(iterator)
    try {
        val iteratorStep = iterator.next()
        if (iteratorStep.done) generatorCoroutineImpl.dropLastIterator()
        return iteratorStep.value
    } catch (e: Throwable) {
        generatorCoroutineImpl.dropLastIterator()
        throw e
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
