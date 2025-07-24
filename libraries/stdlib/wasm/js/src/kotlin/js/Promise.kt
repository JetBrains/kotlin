/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.LowPriorityInOverloadResolution

/**
 * Exposes the JavaScript [Promise object](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Promise) to Kotlin.
 */
@ExperimentalWasmJsInterop
public actual external class Promise<out T : JsAny?>
@SinceKotlin("2.2")
@LowPriorityInOverloadResolution
actual constructor(executor: (resolve: (T) -> Unit, reject: (JsError) -> Unit) -> Unit) : JsAny {

    public constructor(executor: (resolve: (T) -> Unit, reject: (JsAny) -> Unit) -> Unit)

    @LowPriorityInOverloadResolution
    public actual fun <S : JsAny?> then(onFulfilled: ((T) -> S)?): Promise<S>

    @SinceKotlin("2.2")
    @LowPriorityInOverloadResolution
    public actual fun <S : JsAny?> then(onFulfilled: ((T) -> S)?, onRejected: ((JsError) -> S)?): Promise<S>

    @LowPriorityInOverloadResolution
    public fun <S : JsAny?> then(onFulfilled: ((T) -> S)?, onRejected: ((JsAny) -> S)?): Promise<S>

    @SinceKotlin("2.2")
    @LowPriorityInOverloadResolution
    public actual fun <S : JsAny?> catch(onRejected: (JsError) -> S): Promise<S>

    public fun <S : JsAny?> catch(onRejected: (JsAny) -> S): Promise<S>

    public actual fun finally(onFinally: () -> Unit): Promise<T>

    public actual companion object {
        public actual fun <S : JsAny?> all(promise: JsArray<out Promise<S>>): Promise<JsArray<out S>>
        public actual fun <S : JsAny?> race(promise: JsArray<out Promise<S>>): Promise<S>

        @SinceKotlin("2.2")
        @LowPriorityInOverloadResolution
        public actual fun reject(e: JsError): Promise<Nothing>
        public fun reject(e: JsAny): Promise<Nothing>

        public actual fun <S : JsAny?> resolve(e: S): Promise<S>
        public actual fun <S : JsAny?> resolve(e: Promise<S>): Promise<S>
    }
}
