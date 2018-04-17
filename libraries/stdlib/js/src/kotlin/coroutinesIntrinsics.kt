/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.experimental.intrinsics

import kotlin.coroutines.experimental.Continuation

@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
        completion: Continuation<T>
): Any? = this.asDynamic()(completion, false)

@SinceKotlin("1.1")
@Suppress("UNCHECKED_CAST")
@kotlin.internal.InlineOnly
public actual inline fun <R, T> (suspend R.() -> T).startCoroutineUninterceptedOrReturn(
        receiver: R,
        completion: Continuation<T>
): Any? = this.asDynamic()(receiver, completion, false)

@SinceKotlin("1.1")
public actual fun <R, T> (suspend R.() -> T).createCoroutineUnchecked(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit> = this.asDynamic()(receiver, completion, true).facade

@SinceKotlin("1.1")
public actual fun <T> (suspend () -> T).createCoroutineUnchecked(
        completion: Continuation<T>
): Continuation<Unit> = this.asDynamic()(completion, true).facade
