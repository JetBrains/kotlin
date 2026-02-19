/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.LowPriorityInOverloadResolution

/**
 * Exposes the JavaScript [Promise object](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Promise) to Kotlin.
 */
@Suppress("NOT_DOCUMENTED")
public actual open external class Promise<out T>
actual constructor(executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit) {

    @LowPriorityInOverloadResolution
    public actual open fun <S> then(onFulfilled: ((T) -> S)?): Promise<S>

    @LowPriorityInOverloadResolution
    public actual open fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((Throwable) -> S)?): Promise<S>

    public actual open fun <S> catch(onRejected: (Throwable) -> S): Promise<S>

    public actual open fun finally(onFinally: () -> Unit): Promise<T>

    public actual companion object {
        public actual fun <S> all(promise: Array<out Promise<S>>): Promise<Array<out S>>

        public actual fun <S> race(promise: Array<out Promise<S>>): Promise<S>

        public actual fun reject(e: Throwable): Promise<Nothing>

        public actual fun <S> resolve(e: S): Promise<S>
        public actual fun <S> resolve(e: Promise<S>): Promise<S>
    }
}

// It's workaround for KT-19672 since we can fix it properly until KT-11265 isn't fixed.
@Suppress("NOTHING_TO_INLINE")
public inline fun <T, S> Promise<Promise<T>>.then(
    noinline onFulfilled: ((T) -> S)?
): Promise<S> {
    return this.unsafeCast<Promise<T>>().then(onFulfilled)
}

@Suppress("NOTHING_TO_INLINE")
public inline fun <T, S> Promise<Promise<T>>.then(
    noinline onFulfilled: ((T) -> S)?,
    noinline onRejected: ((Throwable) -> S)?
): Promise<S> {
    return this.unsafeCast<Promise<T>>().then(onFulfilled, onRejected)
}
