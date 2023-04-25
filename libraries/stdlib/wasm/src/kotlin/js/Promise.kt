/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.internal.LowPriorityInOverloadResolution

/**
 * Exposes the JavaScript [Promise object](https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/Promise) to Kotlin.
 */
public external class Promise<out T : JsAny?>(executor: (resolve: (T) -> Unit, reject: (JsAny) -> Unit) -> Unit) : JsAny {
    @LowPriorityInOverloadResolution
    public fun <S : JsAny?> then(onFulfilled: ((T) -> S)?): Promise<S>

    @LowPriorityInOverloadResolution
    public fun <S : JsAny?> then(onFulfilled: ((T) -> S)?, onRejected: ((JsAny) -> S)?): Promise<S>

    public fun <S : JsAny?> catch(onRejected: (JsAny) -> S): Promise<S>
    public fun finally(onFinally: () -> Unit): Promise<T>

    companion object {
        public fun <S : JsAny?> all(promise: JsArray<out Promise<S>>): Promise<JsArray<out S>>
        public fun <S : JsAny?> race(promise: JsArray<out Promise<S>>): Promise<S>
        public fun reject(e: JsAny): Promise<Nothing>
        public fun <S : JsAny?> resolve(e: S): Promise<S>
        public fun <S : JsAny?> resolve(e: Promise<S>): Promise<S>
    }
}