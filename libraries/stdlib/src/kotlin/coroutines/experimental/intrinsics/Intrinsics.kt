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

@file:kotlin.jvm.JvmName("IntrinsicsKt")
@file:kotlin.jvm.JvmMultifileClass

package kotlin.coroutines.experimental.intrinsics

import kotlin.coroutines.experimental.*

/**
 * Obtains the current continuation instance inside suspend functions and either suspends
 * currently running coroutine or returns result immediately without suspension.
 *
 * If the [block] returns the special [COROUTINE_SUSPENDED] value, it means that suspend function did suspend the execution and will
 * not return any result immediately. In this case, the [Continuation] provided to the [block] shall be invoked at some moment in the
 * future when the result becomes available to resume the computation.
 *
 * Otherwise, the return value of the [block] must have a type assignable to [T] and represents the result of this suspend function.
 * It means that the execution was not suspended and the [Continuation] provided to the [block] shall not be invoked.
 * As the result type of the [block] is declared as `Any?` and cannot be correctly type-checked,
 * its proper return type remains on the conscience of the suspend function's author.
 *
 * Note that it is not recommended to call either [Continuation.resume] nor [Continuation.resumeWithException] functions synchronously
 * in the same stackframe where suspension function is run. Use [suspendCoroutine] as a safer way to obtain current
 * continuation instance.
 */
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
@kotlin.internal.InlineOnly
public suspend inline fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T =
        throw NotImplementedError("Implementation of suspendCoroutineUninterceptedOrReturn is intrinsic")

/**
 * Intercept continuation with [ContinuationInterceptor].
 */
@SinceKotlin("1.2")
@kotlin.internal.InlineOnly
public inline fun <T> Continuation<T>.intercepted(): Continuation<T> =
        throw NotImplementedError("Implementation of intercepted is intrinsic")

/**
 * Continuation context of current coroutine.
 *
 * This allows the user code to not pass an extra [CoroutineContext] parameter in basic coroutine builders
 * like [launch](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.experimental/launch.html)
 * and [async](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.experimental/async.html),
 * but still provide easy access to coroutine context.
 */
@SinceKotlin("1.2")
@Suppress("WRONG_MODIFIER_TARGET")
@Deprecated(
    "Use kotlin.coroutines.experimental.coroutineContext instead",
    ReplaceWith("kotlin.coroutines.experimental.coroutineContext"),
    DeprecationLevel.WARNING
)
public suspend inline val coroutineContext: CoroutineContext
    get() {
        throw NotImplementedError("Implemented as intrinsic")
    }

/**
 * This value is used as a return value of [suspendCoroutineOrReturn] `block` argument to state that
 * the execution was suspended and will not return any result immediately.
 */
@SinceKotlin("1.1")
public val COROUTINE_SUSPENDED: Any = Any()

