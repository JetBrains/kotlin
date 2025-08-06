/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.js

public open external class Promise<out T>(executor: (resolve: (T) -> Unit, reject: (Throwable) -> Unit) -> Unit) {
    public open fun <S> then(onFulfilled: ((T) -> S)?): kotlin.js.Promise<S>
    public open fun <S> then(onFulfilled: ((T) -> S)?, onRejected: ((Throwable) -> S)?): kotlin.js.Promise<S>

    public open fun <S> catch(onRejected: (Throwable) -> S): kotlin.js.Promise<S>

    public open fun finally(onFinally: () -> Unit): kotlin.js.Promise<T>

    public companion object {
        public fun <S> all(promise: Array<out kotlin.js.Promise<S>>): kotlin.js.Promise<Array<out S>>

        public fun <S> race(promise: Array<out kotlin.js.Promise<S>>): kotlin.js.Promise<S>

        public fun reject(e: Throwable): kotlin.js.Promise<Nothing>

        public fun <S> resolve(e: S): kotlin.js.Promise<S>
        public fun <S> resolve(e: kotlin.js.Promise<S>): kotlin.js.Promise<S>
    }
}