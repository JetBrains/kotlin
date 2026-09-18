/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("RedundantSuspendModifier")

package kotlin.wasm.internal

import kotlin.coroutines.*
import kotlin.internal.DoNotInlineOnFirstStage
import kotlin.internal.InlineOnly
import kotlin.internal.UsedFromCompilerGeneratedCode

@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <T> getContinuation(): Continuation<T> =
    implementedAsIntrinsic

@Suppress("UNCHECKED_CAST")
@UsedFromCompilerGeneratedCode
internal suspend fun <T> returnIfSuspended(argument: Any?): T =
    argument as T

@PublishedApi
@UsedFromCompilerGeneratedCode
internal suspend inline fun getCoroutineContext(): CoroutineContext =
    getCoroutineContextImpl()

@PublishedApi
@UsedFromCompilerGeneratedCode
internal suspend fun getCoroutineContextImpl(): CoroutineContext =
    getContinuation<Any?>().context

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <T> startCoroutineUninterceptedOrReturnIntrinsic0(
    f: (suspend () -> T),
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <R, T> startCoroutineUninterceptedOrReturnIntrinsic1(
    f: (suspend R.() -> T),
    receiver: R,
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <R, P, T> startCoroutineUninterceptedOrReturnIntrinsic2(
    f: (suspend R.(P) -> T),
    receiver: R,
    param: P,
    completion: Continuation<T>
): Any? {
    implementedAsIntrinsic
}

@SinceKotlin("1.3")
@UsedFromCompilerGeneratedCode
internal val EmptyContinuation: Continuation<Any?> = Continuation(EmptyCoroutineContext) { result ->
    val _ = result.getOrThrow()
}

// For State Machine:   (cont as? CoroutineImpl)?.intercepted() ?: cont
// For Stack Switching: (cont as? CoroutineImplStackSwitching<*, *>)?.intercepted() ?: cont
@Suppress("UNUSED_PARAMETER")
@ExcludedFromCodegen
@UsedFromCompilerGeneratedCode
internal fun <T> interceptedIntrinsic(cont: Continuation<T>): Continuation<T> =
    implementedAsIntrinsic

// Is replaced with suspendCoroutineUninterceptedOrReturnStackSwitching or
// suspendCoroutineUninterceptedOrReturnStateMachine
//
// TODO: `suspendCoroutineUninterceptedOrReturn` is replaced and it's body does not play role,
// but for stdlib tests we cannot place there NotImplementedError yet and we use
// outdated suspendCoroutineUninterceptedOrReturnIntrinsic with non-inlinable block.
// Replace the body with `throw NotImplementedError` and remove suppress `USAGE_IS_NOT_INLINABLE`.
@Suppress("UNUSED_PARAMETER", "USAGE_IS_NOT_INLINABLE")
@InlineOnly
@PublishedApi
@DoNotInlineOnFirstStage
@UsedFromCompilerGeneratedCode
internal suspend inline fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T =
    suspendCoroutineUninterceptedOrReturnIntrinsic(block)
// TODO: replace after bootstrap
//    throw NotImplementedError("Suspend coroutine unintercepted")

// TODO: remove after bootstrap
@PublishedApi
@UsedFromCompilerGeneratedCode
internal suspend fun <T> suspendCoroutineUninterceptedOrReturnIntrinsic(block: (Continuation<T>) -> Any?): T =
    returnIfSuspended<T>(block(getContinuation<T>()))
